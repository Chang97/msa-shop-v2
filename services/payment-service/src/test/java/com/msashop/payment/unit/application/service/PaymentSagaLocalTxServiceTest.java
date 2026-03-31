package com.msashop.payment.unit.application.service;

import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.payload.StockReservedPayload;
import com.msashop.payment.application.event.PaymentSagaEventFactory;
import com.msashop.payment.application.port.out.LoadPaymentPort;
import com.msashop.payment.application.port.out.OutboxEventPort;
import com.msashop.payment.application.port.out.ProcessedEventPort;
import com.msashop.payment.application.port.out.SavePaymentPort;
import com.msashop.payment.application.port.out.model.PaymentGatewayResult;
import com.msashop.payment.application.port.out.model.PaymentGatewayStatusResult;
import com.msashop.payment.application.service.PaymentSagaLocalTxService;
import com.msashop.payment.domain.model.PaymentStatus;
import com.msashop.payment.domain.model.PaymentTransaction;
import com.msashop.payment.unit.application.service.PaymentTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentSagaLocalTxServiceTest {

    @Mock
    private LoadPaymentPort loadPaymentPort;

    @Mock
    private SavePaymentPort savePaymentPort;

    @Mock
    private ProcessedEventPort processedEventPort;

    @Mock
    private OutboxEventPort outboxEventPort;

    @Mock
    private PaymentSagaEventFactory paymentSagaEventFactory;

    @InjectMocks
    private PaymentSagaLocalTxService service;

    @Test
    @DisplayName("REQUESTED가 없으면 새 결제 요청 row를 생성한다")
    void should_create_requested_payment_when_not_exists() {
        EventEnvelope source = PaymentTestFixtures.stockReservedEvent();
        StockReservedPayload payload = PaymentTestFixtures.stockReservedPayload();
        PaymentTransaction requested = PaymentTestFixtures.payment(PaymentStatus.REQUESTED);

        when(loadPaymentPort.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(savePaymentPort.save(any())).thenReturn(requested);

        PaymentTransaction result = service.findOrCreateRequested(source, payload);

        assertSame(requested, result);
        verify(savePaymentPort).save(any());
    }

    @Test
    @DisplayName("동시에 저장 충돌이 나면 기존 row를 재조회해서 반환한다")
    void should_reload_existing_payment_when_save_hits_unique_conflict() {
        EventEnvelope source = PaymentTestFixtures.stockReservedEvent();
        StockReservedPayload payload = PaymentTestFixtures.stockReservedPayload();
        PaymentTransaction requested = PaymentTestFixtures.payment(PaymentStatus.REQUESTED);

        when(loadPaymentPort.findByIdempotencyKey("idem-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(requested));
        when(savePaymentPort.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        PaymentTransaction result = service.findOrCreateRequested(source, payload);

        assertSame(requested, result);
    }

    @Test
    @DisplayName("이미 승인된 결제면 승인 이벤트를 다시 적재하고 처리 완료한다")
    void should_complete_when_payment_is_already_approved() {
        EventEnvelope source = PaymentTestFixtures.stockReservedEvent();
        StockReservedPayload payload = PaymentTestFixtures.stockReservedPayload();
        PaymentTransaction approved = PaymentTestFixtures.payment(PaymentStatus.APPROVED).markApproved("pg-tx-1", Instant.now());
        EventEnvelope approvedEvent = PaymentTestFixtures.stockReservedEvent();

        when(paymentSagaEventFactory.paymentApproved(source, payload, approved)).thenReturn(approvedEvent);

        boolean handled = service.completeIfAlreadyHandled("payment-group", "event-1", source, payload, approved);

        assertTrue(handled);
        verify(outboxEventPort).append(approvedEvent);
        verify(processedEventPort).markProcessed(eq("payment-group"), eq("event-1"), any(Instant.class));
    }

    @Test
    @DisplayName("이미 실패된 결제면 실패 이벤트를 다시 적재하고 처리 완료한다")
    void should_complete_when_payment_is_already_failed() {
        EventEnvelope source = PaymentTestFixtures.stockReservedEvent();
        StockReservedPayload payload = PaymentTestFixtures.stockReservedPayload();
        PaymentTransaction failed = PaymentTestFixtures.payment(PaymentStatus.FAILED).markFailed("forced failure", Instant.now());
        EventEnvelope failedEvent = PaymentTestFixtures.stockReservedEvent();

        when(paymentSagaEventFactory.paymentFailed(source, payload, "PAYMENT_ALREADY_FAILED", "forced failure"))
                .thenReturn(failedEvent);

        boolean handled = service.completeIfAlreadyHandled("payment-group", "event-1", source, payload, failed);

        assertTrue(handled);
        verify(outboxEventPort).append(failedEvent);
        verify(processedEventPort).markProcessed(eq("payment-group"), eq("event-1"), any(Instant.class));
    }

    @Test
    @DisplayName("APPROVAL_UNKNOWN이면 outbox 없이 processed만 마킹한다")
    void should_only_mark_processed_when_payment_is_approval_unknown() {
        EventEnvelope source = PaymentTestFixtures.stockReservedEvent();
        StockReservedPayload payload = PaymentTestFixtures.stockReservedPayload();
        PaymentTransaction unknown = PaymentTestFixtures.payment(PaymentStatus.APPROVAL_UNKNOWN).markApprovalUnknown("timeout");

        boolean handled = service.completeIfAlreadyHandled("payment-group", "event-1", source, payload, unknown);

        assertTrue(handled);
        verify(outboxEventPort, never()).append(any());
        verify(processedEventPort).markProcessed(eq("payment-group"), eq("event-1"), any(Instant.class));
    }

    @Test
    @DisplayName("아직 REQUESTED 상태면 즉시 완료하지 않는다")
    void should_return_false_when_payment_is_still_requested() {
        boolean handled = service.completeIfAlreadyHandled(
                "payment-group",
                "event-1",
                PaymentTestFixtures.stockReservedEvent(),
                PaymentTestFixtures.stockReservedPayload(),
                PaymentTestFixtures.payment(PaymentStatus.REQUESTED)
        );

        assertFalse(handled);
        verify(outboxEventPort, never()).append(any());
        verify(processedEventPort, never()).markProcessed(any(), any(), any());
    }

    @Test
    @DisplayName("승인 처리 시 결제 상태 저장, 승인 이벤트 적재, processed 마킹을 수행한다")
    void should_approve_and_mark_processed() {
        EventEnvelope source = PaymentTestFixtures.stockReservedEvent();
        StockReservedPayload payload = PaymentTestFixtures.stockReservedPayload();
        PaymentTransaction requested = PaymentTestFixtures.payment(PaymentStatus.REQUESTED);
        PaymentGatewayResult gatewayResult = new PaymentGatewayResult(true, "FAKE", "pg-tx-1", null, null);
        PaymentTransaction approved = requested.markApproved("pg-tx-1", Instant.now());
        EventEnvelope approvedEvent = PaymentTestFixtures.stockReservedEvent();

        when(savePaymentPort.save(any())).thenReturn(approved);
        when(paymentSagaEventFactory.paymentApproved(source, payload, approved)).thenReturn(approvedEvent);

        service.approveAndMarkProcessed("payment-group", "event-1", source, payload, requested, gatewayResult);

        verify(savePaymentPort).save(any());
        verify(outboxEventPort).append(approvedEvent);
        verify(processedEventPort).markProcessed(eq("payment-group"), eq("event-1"), any(Instant.class));
    }

    @Test
    @DisplayName("실패 처리 시 결제 상태 저장, 실패 이벤트 적재, processed 마킹을 수행한다")
    void should_fail_and_mark_processed() {
        EventEnvelope source = PaymentTestFixtures.stockReservedEvent();
        StockReservedPayload payload = PaymentTestFixtures.stockReservedPayload();
        PaymentTransaction requested = PaymentTestFixtures.payment(PaymentStatus.REQUESTED);
        PaymentGatewayResult gatewayResult = new PaymentGatewayResult(false, "FAKE", null, "PG_FAILED", "forced failure");
        EventEnvelope failedEvent = PaymentTestFixtures.stockReservedEvent();

        when(savePaymentPort.save(any())).thenReturn(requested.markFailed("forced failure", Instant.now()));
        when(paymentSagaEventFactory.paymentFailed(source, payload, "PG_FAILED", "forced failure")).thenReturn(failedEvent);

        service.failAndMarkProcessed("payment-group", "event-1", source, payload, requested, gatewayResult);

        verify(savePaymentPort).save(any());
        verify(outboxEventPort).append(failedEvent);
        verify(processedEventPort).markProcessed(eq("payment-group"), eq("event-1"), any(Instant.class));
    }

    @Test
    @DisplayName("결과가 모호하면 APPROVAL_UNKNOWN으로 저장하고 outbox 없이 processed만 마킹한다")
    void should_mark_approval_unknown_and_processed() {
        PaymentTransaction requested = PaymentTestFixtures.payment(PaymentStatus.REQUESTED);

        when(savePaymentPort.save(any())).thenReturn(requested.markApprovalUnknown("timeout"));

        service.markApprovalUnknownAndProcessed("payment-group", "event-1", requested, new RuntimeException("timeout"));

        verify(savePaymentPort).save(any());
        verify(outboxEventPort, never()).append(any());
        verify(processedEventPort).markProcessed(eq("payment-group"), eq("event-1"), any(Instant.class));
    }

    @Test
    @DisplayName("재조정 승인 시 승인 상태 저장과 승인 이벤트 적재를 수행한다")
    void should_reconcile_approved() {
        PaymentTransaction unknown = PaymentTestFixtures.payment(PaymentStatus.APPROVAL_UNKNOWN).markApprovalUnknown("timeout");
        PaymentGatewayStatusResult status = new PaymentGatewayStatusResult(true, false, "pg-tx-1", null, null);
        PaymentTransaction approved = unknown.markApproved("pg-tx-1", Instant.now());
        EventEnvelope approvedEvent = PaymentTestFixtures.stockReservedEvent();

        when(savePaymentPort.save(any())).thenReturn(approved);
        when(paymentSagaEventFactory.paymentApprovedByReconciliation(approved)).thenReturn(approvedEvent);

        service.reconcileApproved(unknown, status);

        verify(savePaymentPort).save(any());
        verify(outboxEventPort).append(approvedEvent);
    }

    @Test
    @DisplayName("재조정 실패 시 실패 상태 저장과 실패 이벤트 적재를 수행한다")
    void should_reconcile_failed() {
        PaymentTransaction unknown = PaymentTestFixtures.payment(PaymentStatus.APPROVAL_UNKNOWN).markApprovalUnknown("timeout");
        PaymentGatewayStatusResult status = new PaymentGatewayStatusResult(false, true, null, "PG_FAILED", "forced failure");
        PaymentTransaction failed = unknown.markFailed("forced failure", Instant.now());
        EventEnvelope failedEvent = PaymentTestFixtures.stockReservedEvent();

        when(savePaymentPort.save(any())).thenReturn(failed);
        when(paymentSagaEventFactory.paymentFailedByReconciliation(failed, "PG_FAILED", "forced failure")).thenReturn(failedEvent);

        service.reconcileFailed(unknown, status);

        verify(savePaymentPort).save(any());
        verify(outboxEventPort).append(failedEvent);
    }
}
