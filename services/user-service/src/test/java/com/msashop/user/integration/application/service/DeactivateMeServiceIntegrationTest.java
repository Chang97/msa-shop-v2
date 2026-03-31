package com.msashop.user.integration.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventTypes;
import com.msashop.user.adapter.out.persistence.adapter.OutboxEventPersistenceAdapter;
import com.msashop.user.adapter.out.persistence.adapter.UserCommandPersistenceAdapter;
import com.msashop.user.adapter.out.persistence.entity.UserJpaEntity;
import com.msashop.user.adapter.out.persistence.repo.OutboxEventJpaRepository;
import com.msashop.user.adapter.out.persistence.repo.UserCommandJpaRepository;
import com.msashop.user.application.event.UserSagaEventFactory;
import com.msashop.user.application.service.DeactivateMeService;
import com.msashop.user.config.JpaAuditConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        UserCommandPersistenceAdapter.class,
        OutboxEventPersistenceAdapter.class,
        UserSagaEventFactory.class,
        DeactivateMeService.class,
        JpaAuditConfig.class,
        DeactivateMeServiceIntegrationTest.TestConfig.class
})
class DeactivateMeServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("user_service_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private DeactivateMeService service;

    @Autowired
    private UserCommandJpaRepository userCommandJpaRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void resetDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    users,
                    outbox_event,
                    processed_event
                RESTART IDENTITY CASCADE
                """);
    }

    @Test
    @DisplayName("회원 비활성화는 user 상태 변경과 USER_DEACTIVATED outbox 적재를 함께 남긴다")
    void should_deactivate_user_and_append_outbox_event() {
        UserJpaEntity saved = userCommandJpaRepository.save(UserJpaEntity.builder()
                .authUserId(10L)
                .userName("테스트 사용자")
                .empNo("EMP-001")
                .pstnName("개발자")
                .tel("010-1111-2222")
                .useYn(true)
                .build());

        service.deactivateMe(10L);

        assertThat(userCommandJpaRepository.findById(saved.getUserId()).orElseThrow().getUseYn()).isFalse();
        assertThat(outboxEventJpaRepository.findAll())
                .singleElement()
                .extracting(event -> event.getEventType())
                .isEqualTo(EventTypes.USER_DEACTIVATED);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }
}
