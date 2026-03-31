package com.msashop.auth.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.auth.adapter.out.persistence.adapter.ProcessedEventPersistenceAdapter;
import com.msashop.auth.adapter.out.persistence.adapter.UserPersistenceAdapter;
import com.msashop.auth.adapter.out.persistence.entity.AuthUserCredentialJpaEntity;
import com.msashop.auth.adapter.out.persistence.repo.AuthUserCredentialJpaRepository;
import com.msashop.auth.config.JpaAuditConfig;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTopics;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.payload.UserDeactivatedPayload;
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
        UserPersistenceAdapter.class,
        ProcessedEventPersistenceAdapter.class,
        AuthUserSagaCompletionService.class,
        JpaAuditConfig.class,
        AuthUserSagaCompletionServiceIntegrationTest.TestConfig.class
})
class AuthUserSagaCompletionServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auth_service_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private AuthUserSagaCompletionService service;

    @Autowired
    private AuthUserCredentialJpaRepository credentialJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("USER_DEACTIVATED 이벤트를 처리하면 계정을 비활성화하고 processed_event를 완료 처리한다")
    void should_disable_credential_and_mark_processed() throws Exception {
        AuthUserCredentialJpaEntity credential = credentialJpaRepository.save(AuthUserCredentialJpaEntity.builder()
                .email("test@example.com")
                .loginId("tester")
                .passwordHash("hashed-password")
                .enabled(true)
                .build());

        EventEnvelope envelope = new EventEnvelope(
                "event-1",
                EventTypes.USER_DEACTIVATED,
                "USER_PROFILE",
                String.valueOf(credential.getUserId()),
                "saga-1",
                "corr-1",
                "cause-1",
                "user-service",
                EventTopics.AUTH_USER_SAGA_V1,
                String.valueOf(credential.getUserId()),
                Instant.now(),
                objectMapper.writeValueAsString(new UserDeactivatedPayload(credential.getUserId(), 1L))
        );

        boolean handled = service.handle("auth-user-saga-group", "worker-1", 60, envelope);

        assertThat(handled).isTrue();
        assertThat(credentialJpaRepository.findById(credential.getUserId()).orElseThrow().getEnabled()).isFalse();
        assertThat(loadProcessedStatus("auth-user-saga-group", "event-1")).isEqualTo("PROCESSED");
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
