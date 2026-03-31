package com.msashop.payment.unit.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.payload.StockReservedPayload;
import com.msashop.payment.application.port.out.LoadPaymentPort;
import com.msashop.payment.application.port.out.ProcessedEventPort;
import com.msashop.payment.application.port.out.RequestPaymentGatewayPort;
import com.msashop.payment.application.port.out.model.PaymentGatewayRequest;
import com.msashop.payment.application.port.out.model.PaymentGatewayResult;
import com.msashop.payment.application.service.OrderPaymentRequestedSagaService;
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

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @DisplayName("새 결제 요청이 PG 승인되면 APPROVED 흐름을 수행한다")
    void should_approve_payment_and_mark_processed_after_gateway_success() throws Exception {
        EventEnvelope sourceEvent = PaymentTestFixtures.stockReservedEvent();
        StockReservedPayload payload = PaymentTestFixtures.stockReservedPayload();
        PaymentTransaction requested = PaymentTestFixtures.payment(PaymentStatus.REQUESTED);
        PaymentGatewayResult gatewayResult = new PaymentGatewayResult(true, "FAKE", "pg-tx-1", null, null);

        mockClaimSuccess(sourceEvent);
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
    @DisplayName("새 결제 요청이 PG 실패면 FAILED 흐름을 수행한다")
    void should_fail_payment_and_mark_processed_after_gateway_failure() throws Exception {
        EventEnvelope sourceEvent = PaymentTestFixtures.stockReservedEvent();
        StockReservedPayload payload = PaymentTestFixtures.stockReservedPayload();
        PaymentTransaction requested = PaymentTestFixtures.payment(PaymentStatus.REQUESTED);
        PaymentGatewayResult gatewayResult = new PaymentGatewayResult(false, "FAKE", null, "PG_FAILED", "forced failure");

        mockClaimSuccess(sourceEvent);
        when(objectMapper.readValue(sourceEvent.payloadJson(), StockReservedPayload.class)).thenReturn(payload);
        when(loadPaymentPort.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(paymentSagaLocalTxService.findOrCreateRequested(sourceEvent, payload)).thenReturn(requested);
        when(requestPaymentGatewayPort.request(any(PaymentGatewayRequest.class))).thenReturn(gatewayResult);

        boolean handled = service.handle("payment-group", "payment-worker", 300L, sourceEvent);

        assertTrue(handled);
        verify(paymentSagaLocalTxService).failAndMarkProcessed(
                "payment-group",
                "event-1",
                sourceEvent,
                payload,
                requested,
                gatewayResult
        );
    }

    @Test
    @DisplayName("PG 결과가 모호하면 APPROVAL_UNKNOWN으로 남긴다")
    void should_mark_approval_unknown_and_processed_when_gateway_is_ambiguous() throws Exception {
        EventEnvelope sourceEvent = PaymentTestFixtures.stockReservedEvent();
        StockReservedPayload payload = PaymentTestFixtures.stockReservedPayload();
        PaymentTransaction requested = PaymentTestFixtures.payment(PaymentStatus.REQUESTED);

        mockClaimSuccess(sourceEvent);
        when(objectMapper.readValue(sourceEvent.payloadJson(), StockReservedPayload.class)).thenReturn(payload);
        when(loadPaymentPort.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(paymentSagaLocalTxService.findOrCreateRequested(sourceEvent, payload)).thenReturn(requested);
        when(requestPaymentGatewayPort.request(any(PaymentGatewayRequest.class))).thenThrow(new RuntimeException("timeout"));

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

    @Test
    @DisplayName("PG 승인 후 로컬 처리 실패는 APPROVAL_UNKNOWN으로 삼키지 않고 예외를 전파한다")
    void should_propagate_exception_when_local_processing_fails_after_gateway_success() throws Exception {
        EventEnvelope sourceEvent = PaymentTestFixtures.stockReservedEvent();
        StockReservedPayload payload = PaymentTestFixtures.stockReservedPayload();
        PaymentTransaction requested = PaymentTestFixtures.payment(PaymentStatus.REQUESTED);
        PaymentGatewayResult gatewayResult = new PaymentGatewayResult(true, "FAKE", "pg-tx-1", null, null);

        mockClaimSuccess(sourceEvent);
        when(objectMapper.readValue(sourceEvent.payloadJson(), StockReservedPayload.class)).thenReturn(payload);
        when(loadPaymentPort.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(paymentSagaLocalTxService.findOrCreateRequested(sourceEvent, payload)).thenReturn(requested);
        when(requestPaymentGatewayPort.request(any(PaymentGatewayRequest.class))).thenReturn(gatewayResult);
        org.mockito.Mockito.doThrow(new RuntimeException("db failure"))
                .when(paymentSagaLocalTxService)
                .approveAndMarkProcessed("payment-group", "event-1", sourceEvent, payload, requested, gatewayResult);

        assertThrows(RuntimeException.class, () -> service.handle("payment-group", "payment-worker", 300L, sourceEvent));

        verify(paymentSagaLocalTxService, never()).markApprovalUnknownAndProcessed(any(), any(), any(), any());
        verify(processedEventPort).releaseClaim(eq("payment-group"), eq("event-1"), eq("db failure"));
    }

    @Test
    @DisplayName("이미 승인된 결제면 PG를 다시 호출하지 않는다")
    void should_skip_gateway_call_when_existing_payment_is_already_approved() throws Exception {
        EventEnvelope sourceEvent = PaymentTestFixtures.stockReservedEvent();
        StockReservedPayload payload = PaymentTestFixtures.stockReservedPayload();
        PaymentTransaction existing = PaymentTestFixtures.payment(PaymentStatus.APPROVED);

        mockClaimSuccess(sourceEvent);
        when(objectMapper.readValue(sourceEvent.payloadJson(), StockReservedPayload.class)).thenReturn(payload);
        when(loadPaymentPort.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));
        when(paymentSagaLocalTxService.completeIfAlreadyHandled("payment-group", "event-1", sourceEvent, payload, existing))
                .thenReturn(true);

        boolean handled = service.handle("payment-group", "payment-worker", 300L, sourceEvent);

        assertTrue(handled);
        verify(requestPaymentGatewayPort, never()).request(any());
        verify(paymentSagaLocalTxService, never()).findOrCreateRequested(any(), any());
    }

    @Test
    @DisplayName("이미 실패된 결제면 PG를 다시 호출하지 않는다")
    void should_skip_gateway_call_when_existing_payment_is_already_failed() throws Exception {
        EventEnvelope sourceEvent = PaymentTestFixtures.stockReservedEvent();
        StockReservedPayload payload = PaymentTestFixtures.stockReservedPayload();
        PaymentTransaction existing = PaymentTestFixtures.payment(PaymentStatus.FAILED);

        mockClaimSuccess(sourceEvent);
        when(objectMapper.readValue(sourceEvent.payloadJson(), StockReservedPayload.class)).thenReturn(payload);
        when(loadPaymentPort.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));
        when(paymentSagaLocalTxService.completeIfAlreadyHandled("payment-group", "event-1", sourceEvent, payload, existing))
                .thenReturn(true);

        boolean handled = service.handle("payment-group", "payment-worker", 300L, sourceEvent);

        assertTrue(handled);
        verify(requestPaymentGatewayPort, never()).request(any());
        verify(paymentSagaLocalTxService, never()).findOrCreateRequested(any(), any());
    }

    @Test
    @DisplayName("이미 APPROVAL_UNKNOWN이면 PG를 다시 호출하지 않는다")
    void should_skip_gateway_call_when_existing_payment_is_approval_unknown() throws Exception {
        EventEnvelope sourceEvent = PaymentTestFixtures.stockReservedEvent();
        StockReservedPayload payload = PaymentTestFixtures.stockReservedPayload();
        PaymentTransaction existing = PaymentTestFixtures.payment(PaymentStatus.APPROVAL_UNKNOWN);

        mockClaimSuccess(sourceEvent);
        when(objectMapper.readValue(sourceEvent.payloadJson(), StockReservedPayload.class)).thenReturn(payload);
        when(loadPaymentPort.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));
        when(paymentSagaLocalTxService.completeIfAlreadyHandled("payment-group", "event-1", sourceEvent, payload, existing))
                .thenReturn(true);

        boolean handled = service.handle("payment-group", "payment-worker", 300L, sourceEvent);

        assertTrue(handled);
        verify(requestPaymentGatewayPort, never()).request(any());
        verify(paymentSagaLocalTxService, never()).findOrCreateRequested(any(), any());
    }

    private void mockClaimSuccess(EventEnvelope envelope) {
        when(processedEventPort.claim(
                eq("payment-group"),
                eq(envelope),
                eq("payment-worker"),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(true);
    }
}
