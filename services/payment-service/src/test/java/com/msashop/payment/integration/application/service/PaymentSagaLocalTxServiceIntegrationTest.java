package com.msashop.payment.integration.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.payment.adapter.out.persistence.adapter.OutboxEventPersistenceAdapter;
import com.msashop.payment.adapter.out.persistence.adapter.PaymentCommandPersistenceAdapter;
import com.msashop.payment.adapter.out.persistence.adapter.PaymentQueryPersistenceAdapter;
import com.msashop.payment.adapter.out.persistence.adapter.ProcessedEventPersistenceAdapter;
import com.msashop.payment.application.event.PaymentSagaEventFactory;
import com.msashop.payment.application.service.PaymentSagaLocalTxService;
import com.msashop.payment.config.JpaAuditConfig;
import com.msashop.payment.domain.model.PaymentStatus;
import com.msashop.payment.domain.model.PaymentTransaction;
import com.msashop.payment.unit.application.service.PaymentTestFixtures;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        PaymentCommandPersistenceAdapter.class,
        PaymentQueryPersistenceAdapter.class,
        OutboxEventPersistenceAdapter.class,
        ProcessedEventPersistenceAdapter.class,
        PaymentSagaEventFactory.class,
        PaymentSagaLocalTxService.class,
        JpaAuditConfig.class,
        PaymentSagaLocalTxServiceIntegrationTest.TestConfig.class
})
class PaymentSagaLocalTxServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("payment_service_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private PaymentSagaLocalTxService service;

    @Autowired
    private PaymentCommandPersistenceAdapter commandAdapter;

    @Autowired
    private PaymentQueryPersistenceAdapter queryAdapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void resetDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    outbox_event,
                    processed_event,
                    payment_transaction
                RESTART IDENTITY CASCADE
                """);
    }

    @Test
    @DisplayName("승인 처리 시 결제 상태와 outbox, processed_event가 함께 반영된다")
    void should_persist_approved_status_when_approve_and_mark_processed() {
        PaymentTransaction saved = commandAdapter.save(PaymentTransaction.request(
                1L,
                1L,
                new BigDecimal("10000"),
                "KRW",
                "idem-1",
                "FAKE",
                "reservation-1",
                "saga-1",
                "corr-1",
                "event-1"
        ));
        EventEnvelope source = PaymentTestFixtures.stockReservedEvent();
        insertProcessingClaim("payment-group", "event-1", EventTypes.STOCK_RESERVED, source.topic());

        service.approveAndMarkProcessed(
                "payment-group",
                "event-1",
                source,
                PaymentTestFixtures.stockReservedPayload(),
                saved,
                new com.msashop.payment.application.port.out.model.PaymentGatewayResult(true, "FAKE", "pg-tx-1", null, null)
        );

        PaymentTransaction found = queryAdapter.findByIdempotencyKey("idem-1").orElseThrow();
        assertThat(found.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(found.getProviderTxId()).isEqualTo("pg-tx-1");
        assertThat(countOutboxEvents(EventTypes.PAYMENT_APPROVED)).isEqualTo(1);
        assertThat(loadProcessedStatus("payment-group", "event-1")).isEqualTo("PROCESSED");
    }

    @Test
    @DisplayName("결과가 불명확하면 APPROVAL_UNKNOWN과 processed_event만 반영된다")
    void should_persist_approval_unknown_status() {
        PaymentTransaction saved = commandAdapter.save(PaymentTransaction.request(
                1L,
                1L,
                new BigDecimal("10000"),
                "KRW",
                "idem-1",
                "FAKE",
                "reservation-1",
                "saga-1",
                "corr-1",
                "event-1"
        ));
        insertProcessingClaim("payment-group", "event-1", EventTypes.STOCK_RESERVED, PaymentTestFixtures.stockReservedEvent().topic());

        service.markApprovalUnknownAndProcessed(
                "payment-group",
                "event-1",
                saved,
                new RuntimeException("timeout")
        );

        PaymentTransaction found = queryAdapter.findByIdempotencyKey("idem-1").orElseThrow();
        assertThat(found.getStatus()).isEqualTo(PaymentStatus.APPROVAL_UNKNOWN);
        assertThat(found.getFailReason()).isEqualTo("timeout");
        assertThat(countOutboxEvents(null)).isZero();
        assertThat(loadProcessedStatus("payment-group", "event-1")).isEqualTo("PROCESSED");
    }

    private int countOutboxEvents(String eventType) {
        Integer count;
        if (eventType == null) {
            count = jdbcTemplate.queryForObject("select count(*) from outbox_event", Integer.class);
        } else {
            count = jdbcTemplate.queryForObject(
                    "select count(*) from outbox_event where event_type = ?",
                    Integer.class,
                    eventType
            );
        }
        return count == null ? 0 : count;
    }

    private void insertProcessingClaim(String consumerGroup, String eventId, String eventType, String topic) {
        jdbcTemplate.update(
                """
                insert into processed_event (
                    consumer_group,
                    event_id,
                    event_type,
                    topic,
                    status,
                    locked_by,
                    locked_at,
                    processed_at,
                    last_error
                ) values (?, ?, ?, ?, 'PROCESSING', ?, now(), null, null)
                """,
                consumerGroup,
                eventId,
                eventType,
                topic,
                "worker-1"
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
