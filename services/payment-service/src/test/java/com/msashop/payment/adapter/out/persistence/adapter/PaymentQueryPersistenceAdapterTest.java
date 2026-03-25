package com.msashop.payment.adapter.out.persistence.adapter;

import com.msashop.payment.adapter.out.persistence.entity.PaymentTransactionEntity;
import com.msashop.payment.adapter.out.persistence.repo.PaymentQueryJpaRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({
        PaymentQueryPersistenceAdapter.class,
        JpaAuditConfig.class
})
class PaymentQueryPersistenceAdapterTest {

    @Autowired
    private PaymentQueryPersistenceAdapter adapter;

    @Autowired
    private PaymentQueryJpaRepository paymentQueryJpaRepository;

    /**
     * 목적:
     * - idempotencyKey로 payment를 정확히 조회할 수 있는지 확인한다
     * - JPA entity가 domain object로 정상 매핑되는지 확인한다
     *
     * 기대값:
     * - Optional이 비어 있지 않다
     * - 조회한 domain object의 핵심 필드가 저장값과 일치한다
     */
    @Test
    @DisplayName("idempotencyKey로 payment를 조회할 수 있다")
    void should_find_payment_by_idempotency_key() {
        paymentQueryJpaRepository.saveAndFlush(PaymentTransactionEntity.builder()
                .orderId(1L)
                .userId(1L)
                .amount(new BigDecimal("10000"))
                .currency("KRW")
                .idempotencyKey("idem-1")
                .provider("FAKE")
                .providerTxId("pg-tx-1")
                .reservationId("reservation-1")
                .sagaId("saga-1")
                .correlationId("corr-1")
                .sourceEventId("event-1")
                .status(PaymentStatus.APPROVED)
                .requestedAt(Instant.now())
                .approvedAt(Instant.now())
                .build());

        Optional<PaymentTransaction> found = adapter.findByIdempotencyKey("idem-1");

        assertThat(found).isPresent();
        assertThat(found.get().getOrderId()).isEqualTo(1L);
        assertThat(found.get().getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(found.get().getProviderTxId()).isEqualTo("pg-tx-1");
        assertThat(found.get().getReservationId()).isEqualTo("reservation-1");
    }

    /**
     * 목적:
     * - APPROVAL_UNKNOWN 상태 결제만 재조회 대상으로 뽑아오는지 확인한다
     *
     * 기대값:
     * - APPROVAL_UNKNOWN 상태 row만 반환된다
     * - limit 인자를 줘도 adapter가 그 범위 내에서 결과를 돌려준다
     */
    @Test
    @DisplayName("APPROVAL_UNKNOWN 상태 결제만 조회한다")
    void should_find_only_approval_unknown_payments() {
        paymentQueryJpaRepository.saveAndFlush(PaymentTransactionEntity.builder()
                .orderId(1L)
                .userId(1L)
                .amount(new BigDecimal("10000"))
                .currency("KRW")
                .idempotencyKey("idem-unknown-1")
                .provider("FAKE")
                .reservationId("reservation-1")
                .sagaId("saga-1")
                .correlationId("corr-1")
                .sourceEventId("event-1")
                .status(PaymentStatus.APPROVAL_UNKNOWN)
                .requestedAt(Instant.now().minusSeconds(10))
                .build());

        paymentQueryJpaRepository.saveAndFlush(PaymentTransactionEntity.builder()
                .orderId(2L)
                .userId(2L)
                .amount(new BigDecimal("20000"))
                .currency("KRW")
                .idempotencyKey("idem-approved-1")
                .provider("FAKE")
                .providerTxId("pg-tx-2")
                .reservationId("reservation-2")
                .sagaId("saga-2")
                .correlationId("corr-2")
                .sourceEventId("event-2")
                .status(PaymentStatus.APPROVED)
                .requestedAt(Instant.now())
                .approvedAt(Instant.now())
                .build());

        List<PaymentTransaction> found = adapter.findApprovalUnknown(20);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getIdempotencyKey()).isEqualTo("idem-unknown-1");
        assertThat(found.get(0).getStatus()).isEqualTo(PaymentStatus.APPROVAL_UNKNOWN);
    }
}