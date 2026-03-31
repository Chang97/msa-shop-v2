package com.msashop.payment.integration.adapter.out.persistence.adapter;

import com.msashop.payment.adapter.out.persistence.adapter.PaymentCommandPersistenceAdapter;
import com.msashop.payment.adapter.out.persistence.adapter.PaymentQueryPersistenceAdapter;
import com.msashop.payment.config.JpaAuditConfig;
import com.msashop.payment.domain.model.PaymentStatus;
import com.msashop.payment.domain.model.PaymentTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        PaymentCommandPersistenceAdapter.class,
        PaymentQueryPersistenceAdapter.class,
        JpaAuditConfig.class
})
class PaymentCommandPersistenceAdapterTest {

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
    @DisplayName("결제 요청 저장 후 다시 조회할 수 있다")
    void should_save_requested_payment() {
        PaymentTransaction requested = PaymentTransaction.request(
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
        );

        PaymentTransaction saved = commandAdapter.save(requested);

        assertThat(saved.getPaymentId()).isNotNull();
        assertThat(queryAdapter.findByIdempotencyKey("idem-1").isPresent()).isTrue();
        assertThat(queryAdapter.findByIdempotencyKey("idem-1").get().getStatus()).isEqualTo(PaymentStatus.REQUESTED);
    }

    @Test
    @DisplayName("저장된 결제를 승인 상태로 갱신할 수 있다")
    void should_update_payment_status_to_approved() {
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

        PaymentTransaction approved = commandAdapter.save(saved.markApproved("pg-tx-1", Instant.now()));

        assertThat(approved.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(queryAdapter.findByIdempotencyKey("idem-1").isPresent()).isTrue();
        assertThat(queryAdapter.findByIdempotencyKey("idem-1").get().getProviderTxId()).isEqualTo("pg-tx-1");
    }
}
