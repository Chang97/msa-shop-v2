package com.msashop.user.integration.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTopics;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.payload.AuthUserCreatedPayload;
import com.msashop.user.adapter.out.persistence.entity.OutboxEventJpaEntity;
import com.msashop.user.application.event.UserSagaEventFactory;
import com.msashop.user.adapter.out.persistence.adapter.OutboxEventPersistenceAdapter;
import com.msashop.user.adapter.out.persistence.adapter.ProcessedEventPersistenceAdapter;
import com.msashop.user.adapter.out.persistence.adapter.UserProfileCreatePersistenceAdapter;
import com.msashop.user.adapter.out.persistence.repo.OutboxEventJpaRepository;
import com.msashop.user.adapter.out.persistence.repo.UserCommandJpaRepository;
import com.msashop.user.application.service.AuthUserCreatedSagaService;
import com.msashop.user.application.service.ProvisionUserProfileService;
import com.msashop.user.config.JpaAuditConfig;
import org.junit.jupiter.api.AfterEach;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        UserProfileCreatePersistenceAdapter.class,
        OutboxEventPersistenceAdapter.class,
        ProcessedEventPersistenceAdapter.class,
        UserSagaEventFactory.class,
        ProvisionUserProfileService.class,
        AuthUserCreatedSagaService.class,
        JpaAuditConfig.class,
        AuthUserCreatedSagaServiceIntegrationTest.TestConfig.class
})
class AuthUserCreatedSagaServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("user_service_auth_user_created_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private AuthUserCreatedSagaService service;

    @Autowired
    private UserCommandJpaRepository userCommandJpaRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

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
    @DisplayName("AUTH_USER_CREATED 처리 성공 시 프로필 생성, USER_PROFILE_CREATED 적재, processed 완료가 함께 반영된다")
    void should_create_profile_and_append_success_outbox_and_mark_processed() throws Exception {
        EventEnvelope envelope = new EventEnvelope(
                "event-1",
                EventTypes.AUTH_USER_CREATED,
                "AUTH_USER",
                "10",
                "saga-1",
                "corr-1",
                null,
                "auth-service",
                EventTopics.AUTH_USER_SAGA_V1,
                "10",
                Instant.now(),
                objectMapper.writeValueAsString(new AuthUserCreatedPayload(
                        10L,
                        "홍길동",
                        "EMP-001",
                        "개발팀",
                        "010-1111-2222"
                ))
        );

        boolean handled = service.handle("user-service-auth-user-saga", "worker-1", 300L, envelope);

        assertThat(handled).isTrue();
        assertThat(userCommandJpaRepository.findByAuthUserId(10L)).isPresent();
        assertThat(outboxEventJpaRepository.findAll())
                .singleElement()
                .extracting(OutboxEventJpaEntity::getEventType)
                .isEqualTo(EventTypes.USER_PROFILE_CREATED);
        assertThat(loadProcessedStatus("user-service-auth-user-saga", "event-1")).isEqualTo("PROCESSED");
    }

    @Test
    @DisplayName("같은 authUserId의 AUTH_USER_CREATED가 중복 도착해도 사용자 프로필 row는 하나만 유지된다")
    void should_keep_single_user_profile_when_auth_user_created_is_received_twice() throws Exception {
        EventEnvelope first = new EventEnvelope(
                "event-1",
                EventTypes.AUTH_USER_CREATED,
                "AUTH_USER",
                "10",
                "saga-1",
                "corr-1",
                null,
                "auth-service",
                EventTopics.AUTH_USER_SAGA_V1,
                "10",
                Instant.now(),
                objectMapper.writeValueAsString(new AuthUserCreatedPayload(
                        10L,
                        "홍길동",
                        "EMP-001",
                        "개발팀",
                        "010-1111-2222"
                ))
        );
        EventEnvelope second = new EventEnvelope(
                "event-2",
                EventTypes.AUTH_USER_CREATED,
                "AUTH_USER",
                "10",
                "saga-1",
                "corr-1",
                null,
                "auth-service",
                EventTopics.AUTH_USER_SAGA_V1,
                "10",
                Instant.now(),
                objectMapper.writeValueAsString(new AuthUserCreatedPayload(
                        10L,
                        "홍길동",
                        "EMP-001",
                        "개발팀",
                        "010-1111-2222"
                ))
        );

        boolean firstHandled = service.handle("user-service-auth-user-saga", "worker-1", 300L, first);
        boolean secondHandled = service.handle("user-service-auth-user-saga", "worker-1", 300L, second);

        assertThat(firstHandled).isTrue();
        assertThat(secondHandled).isTrue();
        assertThat(countUsersByAuthUserId(10L)).isEqualTo(1);
        assertThat(userCommandJpaRepository.findByAuthUserId(10L)).isPresent();
        assertThat(outboxEventJpaRepository.findAll()).hasSize(2);
        assertThat(loadProcessedStatus("user-service-auth-user-saga", "event-1")).isEqualTo("PROCESSED");
        assertThat(loadProcessedStatus("user-service-auth-user-saga", "event-2")).isEqualTo("PROCESSED");
    }

    private long countUsersByAuthUserId(Long authUserId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from users where auth_user_id = ?",
                Long.class,
                authUserId
        );
    }

    private String loadProcessedStatus(String consumerGroup, String eventId) {
        return jdbcTemplate.queryForObject(
                "select status from processed_event where consumer_group = ? and event_id = ?",
                String.class,
                consumerGroup,
                eventId
        );
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }
}
