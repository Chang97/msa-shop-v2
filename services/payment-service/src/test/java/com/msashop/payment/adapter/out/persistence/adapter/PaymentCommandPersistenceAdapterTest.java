package com.msashop.payment.adapter.out.persistence.adapter;

import com.msashop.payment.config.JpaAuditConfig;
import com.msashop.payment.domain.model.PaymentStatus;
import com.msashop.payment.domain.model.PaymentTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({
        PaymentCommandPersistenceAdapter.class,
        PaymentQueryPersistenceAdapter.class,
        JpaAuditConfig.class
})
class PaymentCommandPersistenceAdapterTest {

    @Autowired
    private PaymentCommandPersistenceAdapter commandAdapter;

    @Autowired
    private PaymentQueryPersistenceAdapter queryAdapter;

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
