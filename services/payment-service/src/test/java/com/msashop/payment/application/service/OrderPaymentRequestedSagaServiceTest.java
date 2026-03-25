package com.msashop.payment.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.payload.StockReservedPayload;
import com.msashop.payment.application.event.PaymentSagaEventFactory;
import com.msashop.payment.application.port.out.LoadPaymentPort;
import com.msashop.payment.application.port.out.OutboxEventPort;
import com.msashop.payment.application.port.out.ProcessedEventPort;
import com.msashop.payment.application.port.out.RequestPaymentGatewayPort;
import com.msashop.payment.application.port.out.SavePaymentPort;
import com.msashop.payment.application.port.out.model.PaymentGatewayRequest;
import com.msashop.payment.application.port.out.model.PaymentGatewayResult;
import com.msashop.payment.domain.model.PaymentStatus;
import com.msashop.payment.domain.model.PaymentTransaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrderPaymentRequestedSagaService 단위 테스트.
 *
 * 검증 목적:
 * - payment-service가 StockReserved 이후 결제를 어떻게 처리하는지
 * - 결제 성공, 애매한 PG 실패(APPROVAL_UNKNOWN) 분기를 올바르게 나누는지
 *
 * 핵심 포인트:
 * - PG 호출 전 REQUESTED 저장
 * - 성공 시 PaymentApproved 이벤트 적재
 * - 애매한 실패 시 APPROVAL_UNKNOWN 저장 후 즉시 결과 이벤트는 발행하지 않음
 */
@ExtendWith(MockitoExtension.class)
class OrderPaymentRequestedSagaServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProcessedEventPort processedEventPort;

    @Mock
    private LoadPaymentPort loadPaymentPort;

    @Mock
    private SavePaymentPort savePaymentPort;

    @Mock
    private RequestPaymentGatewayPort requestPaymentGatewayPort;

    @Mock
    private OutboxEventPort outboxEventPort;

    @Mock
    private PaymentSagaEventFactory paymentSagaEventFactory;

    @InjectMocks
    private OrderPaymentRequestedSagaService service;

    /**
     * 결제 승인 성공 케이스.
     *
     * 시나리오:
     * - StockReserved 이벤트 수신
     * - 동일 idempotencyKey의 기존 결제 없음
     * - PG가 승인 성공 반환
     *
     * 기대값:
     * - payment row가 REQUESTED -> APPROVED로 저장된다
     * - PaymentApproved 이벤트가 outbox에 적재된다
     * - 원본 이벤트는 processed 처리된다
     * - claim은 release 되지 않는다
     */
    @Test
    void should_approve_payment_and_append_payment_approved_event() throws Exception {
        EventEnvelope sourceEvent = stockReservedEvent();
        StockReservedPayload payload = stockReservedPayload();

        PaymentTransaction requested = payment(
                100L, PaymentStatus.REQUESTED, null, null, null
        );
        PaymentTransaction approved = payment(
                100L, PaymentStatus.APPROVED, "pg-tx-1", Instant.now(), null
        );

        EventEnvelope approvedEvent = new EventEnvelope(
                "event-2",
                EventTypes.PAYMENT_APPROVED,
                "PAYMENT",
                "100",
                "saga-1",
                "corr-1",
                "event-1",
                "payment-service",
                "order.payment.saga.v1",
                "1",
                Instant.now(),
                "{\"orderId\":1}"
        );

        when(processedEventPort.claim(
                eq("payment-group"),
                eq(sourceEvent),
                eq("payment-worker"),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(true);
        when(objectMapper.readValue(sourceEvent.payloadJson(), StockReservedPayload.class)).thenReturn(payload);
        when(loadPaymentPort.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(savePaymentPort.save(any())).thenReturn(requested, approved);
        when(requestPaymentGatewayPort.request(any(PaymentGatewayRequest.class)))
                .thenReturn(new PaymentGatewayResult(true, "FAKE", "pg-tx-1", null, null));
        when(paymentSagaEventFactory.paymentApproved(sourceEvent, payload, approved)).thenReturn(approvedEvent);

        boolean handled = service.handle("payment-group", "payment-worker", 300L, sourceEvent);

        assertTrue(handled);
        verify(outboxEventPort).append(approvedEvent);
        verify(processedEventPort).markProcessed(eq("payment-group"), eq("event-1"), any(Instant.class));
        verify(processedEventPort, never()).releaseClaim(any(), any(), any());
    }

    /**
     * PG 결과가 애매한 실패 케이스.
     *
     * 시나리오:
     * - StockReserved 이벤트 수신
     * - PG 호출 중 timeout/네트워크 예외 발생
     *
     * 기대값:
     * - payment row는 APPROVAL_UNKNOWN으로 남는다
     * - PaymentApproved / PaymentFailed 이벤트는 즉시 발행하지 않는다
     * - 원본 이벤트는 processed 처리된다
     * - claim을 release 해서 즉시 재시도하지 않는다
     *
     * 이유:
     * - 이미 PG에 요청이 들어갔을 수 있으므로 중복 승인 위험이 있다
     * - 최종 판정은 reconciliation이 맡아야 한다
     */
    @Test
    void should_mark_approval_unknown_and_not_append_event_when_gateway_is_ambiguous() throws Exception {
        EventEnvelope sourceEvent = stockReservedEvent();
        StockReservedPayload payload = stockReservedPayload();

        PaymentTransaction requested = payment(
                100L, PaymentStatus.REQUESTED, null, null, null
        );
        PaymentTransaction unknown = payment(
                100L, PaymentStatus.APPROVAL_UNKNOWN, null, null, "timeout"
        );

        when(processedEventPort.claim(
                eq("payment-group"),
                eq(sourceEvent),
                eq("payment-worker"),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(true);
        when(objectMapper.readValue(sourceEvent.payloadJson(), StockReservedPayload.class)).thenReturn(payload);
        when(loadPaymentPort.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(savePaymentPort.save(any())).thenReturn(requested, unknown);
        when(requestPaymentGatewayPort.request(any(PaymentGatewayRequest.class)))
                .thenThrow(new RuntimeException("timeout"));

        boolean handled = service.handle("payment-group", "payment-worker", 300L, sourceEvent);

        assertTrue(handled);
        verify(outboxEventPort, never()).append(any());
        verify(processedEventPort).markProcessed(eq("payment-group"), eq("event-1"), any(Instant.class));
        verify(processedEventPort, never()).releaseClaim(any(), any(), any());
    }

    private EventEnvelope stockReservedEvent() {
        return new EventEnvelope(
                "event-1",
                EventTypes.STOCK_RESERVED,
                "STOCK_RESERVATION",
                "reservation-1",
                "saga-1",
                "corr-1",
                "cause-1",
                "product-service",
                "order.payment.saga.v1",
                "1",
                Instant.now(),
                "{\"orderId\":1}"
        );
    }

    private StockReservedPayload stockReservedPayload() {
        return new StockReservedPayload(
                1L,
                1L,
                "reservation-1",
                new BigDecimal("10000"),
                "KRW",
                "idem-1",
                "FAKE"
        );
    }

    private PaymentTransaction payment(
            Long paymentId,
            PaymentStatus status,
            String providerTxId,
            Instant approvedAt,
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
                "event-1",
                status,
                Instant.now(),
                approvedAt,
                null,
                failReason,
                Instant.now(),
                Instant.now()
        );
    }
}