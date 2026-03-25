package com.msashop.payment.application.service;

import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.payment.application.event.PaymentSagaEventFactory;
import com.msashop.payment.application.port.out.GetPaymentStatusGatewayPort;
import com.msashop.payment.application.port.out.LoadPaymentPort;
import com.msashop.payment.application.port.out.OutboxEventPort;
import com.msashop.payment.application.port.out.SavePaymentPort;
import com.msashop.payment.application.port.out.model.PaymentGatewayStatusResult;
import com.msashop.payment.domain.model.PaymentStatus;
import com.msashop.payment.domain.model.PaymentTransaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PaymentReconciliationService 단위 테스트.
 *
 * 검증 목적:
 * - APPROVAL_UNKNOWN 상태의 결제를 PG 재조회로 최종 확정할 수 있는지 검증한다
 *
 * 핵심 정책:
 * - 재조회 승인 -> APPROVED 저장 + PaymentApproved 발행
 * - 재조회 실패 -> FAILED 저장 + PaymentFailed 발행
 */
@ExtendWith(MockitoExtension.class)
class PaymentReconciliationServiceTest {

    @Mock
    private LoadPaymentPort loadPaymentPort;

    @Mock
    private SavePaymentPort savePaymentPort;

    @Mock
    private GetPaymentStatusGatewayPort getPaymentStatusGatewayPort;

    @Mock
    private OutboxEventPort outboxEventPort;

    @Mock
    private PaymentSagaEventFactory paymentSagaEventFactory;

    @InjectMocks
    private PaymentReconciliationService service;

    /**
     * 미확정 결제가 재조회 후 승인으로 확정되는 케이스.
     *
     * 시나리오:
     * - APPROVAL_UNKNOWN payment 존재
     * - PG 상태 조회 결과 approved
     *
     * 기대값:
     * - payment가 APPROVED로 저장된다
     * - PaymentApproved 이벤트가 outbox에 적재된다
     */
    @Test
    void should_append_payment_approved_event_when_unknown_payment_is_confirmed() {
        PaymentTransaction unknown = payment(
                100L,
                PaymentStatus.APPROVAL_UNKNOWN,
                null,
                null
        );

        PaymentTransaction approved = payment(
                100L,
                PaymentStatus.APPROVED,
                "pg-tx-recon-1",
                null
        );

        EventEnvelope approvedEvent = new EventEnvelope(
                "event-1",
                EventTypes.PAYMENT_APPROVED,
                "PAYMENT",
                "100",
                "saga-1",
                "corr-1",
                "event-0",
                "payment-service",
                "order.payment.saga.v1",
                "1",
                Instant.now(),
                "{\"orderId\":1}"
        );

        when(loadPaymentPort.findApprovalUnknown(20)).thenReturn(List.of(unknown));
        when(getPaymentStatusGatewayPort.getStatus("FAKE", "idem-1", null))
                .thenReturn(new PaymentGatewayStatusResult(true, false, "pg-tx-recon-1", null, null));
        when(savePaymentPort.save(any())).thenReturn(approved);
        when(paymentSagaEventFactory.paymentApprovedByReconciliation(approved)).thenReturn(approvedEvent);

        service.reconcile(20);

        verify(outboxEventPort).append(approvedEvent);
    }

    /**
     * 미확정 결제가 재조회 후 실패로 확정되는 케이스.
     *
     * 시나리오:
     * - APPROVAL_UNKNOWN payment 존재
     * - PG 상태 조회 결과 failed
     *
     * 기대값:
     * - payment가 FAILED로 저장된다
     * - PaymentFailed 이벤트가 outbox에 적재된다
     */
    @Test
    void should_append_payment_failed_event_when_unknown_payment_is_resolved_as_failed() {
        PaymentTransaction unknown = payment(
                100L,
                PaymentStatus.APPROVAL_UNKNOWN,
                null,
                null
        );

        PaymentTransaction failed = payment(
                100L,
                PaymentStatus.FAILED,
                null,
                "PG_RECON_FAILED"
        );

        EventEnvelope failedEvent = new EventEnvelope(
                "event-2",
                EventTypes.PAYMENT_FAILED,
                "PAYMENT",
                "100",
                "saga-1",
                "corr-1",
                "event-0",
                "payment-service",
                "order.payment.saga.v1",
                "1",
                Instant.now(),
                "{\"orderId\":1}"
        );

        when(loadPaymentPort.findApprovalUnknown(20)).thenReturn(List.of(unknown));
        when(getPaymentStatusGatewayPort.getStatus("FAKE", "idem-1", null))
                .thenReturn(new PaymentGatewayStatusResult(false, true, null, "PG_RECON_FAILED", "forced failure"));
        when(savePaymentPort.save(any())).thenReturn(failed);
        when(paymentSagaEventFactory.paymentFailedByReconciliation(
                failed,
                "PG_RECON_FAILED",
                "forced failure"
        )).thenReturn(failedEvent);

        service.reconcile(20);

        verify(outboxEventPort).append(failedEvent);
    }

    private PaymentTransaction payment(
            Long paymentId,
            PaymentStatus status,
            String providerTxId,
            String failReason
    ) {
        return PaymentTransaction.rehydrate(
                paymentId,
                1L,
                1L,
                new BigDecimal("10000"),
                "KRW",
                "idem-1",
                "FAKE",
                providerTxId,
                "reservation-1",
                "saga-1",
                "corr-1",
                "event-0",
                status,
                Instant.now(),
                null,
                null,
                failReason,
                Instant.now(),
                Instant.now()
        );
    }
}