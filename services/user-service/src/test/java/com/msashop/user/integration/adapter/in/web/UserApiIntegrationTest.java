package com.msashop.user.integration.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventTypes;
import com.msashop.user.adapter.in.web.dto.UserMeUpdateRequest;
import com.msashop.user.adapter.out.persistence.entity.OutboxEventJpaEntity;
import com.msashop.user.adapter.out.persistence.entity.UserJpaEntity;
import com.msashop.user.adapter.out.persistence.repo.OutboxEventJpaRepository;
import com.msashop.user.adapter.out.persistence.repo.UserCommandJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.kafka.consumers.auth-user-saga-enabled=false",
        "app.kafka.producer.relay-enabled=false"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class UserApiIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("user_service_api_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserCommandJpaRepository userCommandJpaRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    users,
                    outbox_event,
                    processed_event
                RESTART IDENTITY CASCADE
                """);
    }

    @Test
    @DisplayName("users/me 조회 성공 시 gateway 헤더 사용자 기준으로 프로필을 반환한다")
    void should_return_current_user_profile() throws Exception {
        UserJpaEntity saved = userCommandJpaRepository.save(UserJpaEntity.builder()
                .authUserId(10L)
                .userName("홍길동")
                .empNo("EMP-001")
                .pstnName("개발팀")
                .tel("010-1111-2222")
                .useYn(true)
                .build());

        mockMvc.perform(get("/api/users/me")
                        .header("X-User-Id", 10L)
                        .header("X-Roles", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(saved.getUserId()))
                .andExpect(jsonPath("$.authUserId").value(10L))
                .andExpect(jsonPath("$.userName").value("홍길동"))
                .andExpect(jsonPath("$.empNo").value("EMP-001"))
                .andExpect(jsonPath("$.pstnName").value("개발팀"))
                .andExpect(jsonPath("$.tel").value("010-1111-2222"))
                .andExpect(jsonPath("$.useYn").value(true));
    }

    @Test
    @DisplayName("users/me 수정 성공 시 현재 사용자 프로필만 변경한다")
    void should_update_current_user_profile() throws Exception {
        UserJpaEntity saved = userCommandJpaRepository.save(UserJpaEntity.builder()
                .authUserId(10L)
                .userName("홍길동")
                .empNo("EMP-001")
                .pstnName("개발팀")
                .tel("010-1111-2222")
                .useYn(true)
                .build());

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", 10L)
                        .header("X-Roles", "ROLE_USER")
                        .content(objectMapper.writeValueAsString(new UserMeUpdateRequest(
                                "김철수",
                                null,
                                "플랫폼팀",
                                null
                        ))))
                .andExpect(status().isNoContent());

        UserJpaEntity updated = userCommandJpaRepository.findById(saved.getUserId()).orElseThrow();
        assertThat(updated.getUserName()).isEqualTo("김철수");
        assertThat(updated.getEmpNo()).isNull();
        assertThat(updated.getPstnName()).isEqualTo("플랫폼팀");
        assertThat(updated.getTel()).isNull();
    }

    @Test
    @DisplayName("users/me/deactivate 성공 시 프로필을 비활성화하고 USER_DEACTIVATED outbox를 남긴다")
    void should_deactivate_current_user_and_append_outbox_event() throws Exception {
        UserJpaEntity saved = userCommandJpaRepository.save(UserJpaEntity.builder()
                .authUserId(10L)
                .userName("홍길동")
                .empNo("EMP-001")
                .pstnName("개발팀")
                .tel("010-1111-2222")
                .useYn(true)
                .build());

        mockMvc.perform(patch("/api/users/me/deactivate")
                        .header("X-User-Id", 10L)
                        .header("X-Roles", "ROLE_USER"))
                .andExpect(status().isNoContent());

        assertThat(userCommandJpaRepository.findById(saved.getUserId()).orElseThrow().getUseYn()).isFalse();
        assertThat(outboxEventJpaRepository.findAll())
                .singleElement()
                .extracting(OutboxEventJpaEntity::getEventType)
                .isEqualTo(EventTypes.USER_DEACTIVATED);
    }
}
