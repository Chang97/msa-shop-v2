package com.msashop.payment.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.payload.StockReservedPayload;
import com.msashop.payment.application.port.out.LoadPaymentPort;
import com.msashop.payment.application.port.out.ProcessedEventPort;
import com.msashop.payment.application.port.out.RequestPaymentGatewayPort;
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

@ExtendWith(MockitoExtension.class)
class OrderPaymentRequestedSagaServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProcessedEventPort processedEventPort;

    @Mock
    private LoadPaymentPort loadPaymentPort;

    @Mock
    private RequestPaymentGatewayPort requestPaymentGatewayPort;

    @Mock
    private PaymentSagaLocalTxService paymentSagaLocalTxService;

    @InjectMocks
    private OrderPaymentRequestedSagaService service;

    @Test
    void should_approve_payment_and_mark_processed_after_gateway_success() throws Exception {
        EventEnvelope sourceEvent = stockReservedEvent();
        StockReservedPayload payload = stockReservedPayload();
        PaymentTransaction requested = payment(100L, PaymentStatus.REQUESTED, null, null, null);
        PaymentGatewayResult gatewayResult = new PaymentGatewayResult(true, "FAKE", "pg-tx-1", null, null);

        when(processedEventPort.claim(
                eq("payment-group"),
                eq(sourceEvent),
                eq("payment-worker"),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(true);
        when(objectMapper.readValue(sourceEvent.payloadJson(), StockReservedPayload.class)).thenReturn(payload);
        when(loadPaymentPort.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(paymentSagaLocalTxService.findOrCreateRequested(sourceEvent, payload)).thenReturn(requested);
        when(requestPaymentGatewayPort.request(any(PaymentGatewayRequest.class))).thenReturn(gatewayResult);

        boolean handled = service.handle("payment-group", "payment-worker", 300L, sourceEvent);

        assertTrue(handled);
        verify(paymentSagaLocalTxService).approveAndMarkProcessed(
                "payment-group",
                "event-1",
                sourceEvent,
                payload,
                requested,
                gatewayResult
        );
        verify(processedEventPort, never()).releaseClaim(any(), any(), any());
    }

    @Test
    void should_mark_approval_unknown_and_processed_when_gateway_is_ambiguous() throws Exception {
        EventEnvelope sourceEvent = stockReservedEvent();
        StockReservedPayload payload = stockReservedPayload();
        PaymentTransaction requested = payment(100L, PaymentStatus.REQUESTED, null, null, null);

        when(processedEventPort.claim(
                eq("payment-group"),
                eq(sourceEvent),
                eq("payment-worker"),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(true);
        when(objectMapper.readValue(sourceEvent.payloadJson(), StockReservedPayload.class)).thenReturn(payload);
        when(loadPaymentPort.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(paymentSagaLocalTxService.findOrCreateRequested(sourceEvent, payload)).thenReturn(requested);
        when(requestPaymentGatewayPort.request(any(PaymentGatewayRequest.class)))
                .thenThrow(new RuntimeException("timeout"));

        boolean handled = service.handle("payment-group", "payment-worker", 300L, sourceEvent);

        assertTrue(handled);
        verify(paymentSagaLocalTxService).markApprovalUnknownAndProcessed(
                eq("payment-group"),
                eq("event-1"),
                eq(requested),
                any(RuntimeException.class)
        );
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
