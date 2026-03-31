package com.msashop.product.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.payload.PaymentApprovedPayload;
import com.msashop.common.event.payload.PaymentFailedPayload;
import com.msashop.common.event.payload.StockReservationItemPayload;
import com.msashop.common.event.payload.StockReservationRequestedPayload;
import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.PaymentErrorCode;
import com.msashop.product.application.event.ProductSagaEventFactory;
import com.msashop.product.application.port.out.OutboxEventPort;
import com.msashop.product.application.port.out.ProcessedEventPort;
import com.msashop.product.application.port.out.StockReservationPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 주문-결제 saga에서 product-service가 맡는 재고 예약 흐름을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class OrderPaymentSagaServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProcessedEventPort processedEventPort;

    @Mock
    private StockReservationPort stockReservationPort;

    @Mock
    private StockReservationLocalTxService stockReservationLocalTxService;

    @Mock
    private OutboxEventPort outboxEventPort;

    @Mock
    private ProductSagaEventFactory productSagaEventFactory;

    @InjectMocks
    private OrderPaymentSagaService service;

    /**
     * 신규 예약 요청이면 재고를 예약하고 STOCK_RESERVED 이벤트를 적재해야 한다.
     */
    @Test
    @DisplayName("신규 예약 요청이면 재고를 예약하고 STOCK_RESERVED 이벤트를 적재한다")
    void should_reserve_stock_and_append_stock_reserved_event() throws Exception {
        EventEnvelope sourceEvent = stockReservationRequestedEvent();
        StockReservationRequestedPayload payload = stockReservationRequestedPayload();
        EventEnvelope reservedEvent = stockReservedEvent();

        mockClaimSuccess(sourceEvent);
        when(objectMapper.readValue(sourceEvent.payloadJson(), StockReservationRequestedPayload.class)).thenReturn(payload);
        when(stockReservationPort.findActiveReservationId(1L)).thenReturn(Optional.empty());
        when(productSagaEventFactory.stockReserved(eq(sourceEvent), eq(payload), anyString())).thenReturn(reservedEvent);

        boolean handled = service.handle("product-group", "product-worker", 300L, sourceEvent);

        assertTrue(handled);
        verify(stockReservationLocalTxService).reserve(anyString(), eq(1L), eq(payload.items()));
        verify(outboxEventPort).append(reservedEvent);
        verify(processedEventPort).markProcessed(eq("product-group"), eq("event-1"), any(Instant.class));
        verify(processedEventPort, never()).releaseClaim(anyString(), anyString(), anyString());
    }

    /**
     * 이미 활성 예약이 있으면 새 예약을 만들지 않고 기존 reservationId를 재사용해야 한다.
     */
    @Test
    @DisplayName("활성 예약이 이미 있으면 기존 reservationId를 재사용한다")
    void should_reuse_existing_reservation_when_active_reservation_exists() throws Exception {
        EventEnvelope sourceEvent = stockReservationRequestedEvent();
        StockReservationRequestedPayload payload = stockReservationRequestedPayload();
        EventEnvelope reservedEvent = stockReservedEvent();

        mockClaimSuccess(sourceEvent);
        when(objectMapper.readValue(sourceEvent.payloadJson(), StockReservationRequestedPayload.class)).thenReturn(payload);
        when(stockReservationPort.findActiveReservationId(1L)).thenReturn(Optional.of("reservation-existing"));
        when(productSagaEventFactory.stockReserved(sourceEvent, payload, "reservation-existing")).thenReturn(reservedEvent);

        boolean handled = service.handle("product-group", "product-worker", 300L, sourceEvent);

        assertTrue(handled);
        verify(stockReservationLocalTxService, never()).reserve(anyString(), anyLong(), any());
        verify(outboxEventPort).append(reservedEvent);
        verify(processedEventPort).markProcessed(eq("product-group"), eq("event-1"), any(Instant.class));
    }

    /**
     * 재고 부족 같은 비즈니스 실패는 실패 이벤트를 적재하고 processed 처리해야 한다.
     */
    @Test
    @DisplayName("재고 부족이면 STOCK_RESERVATION_FAILED 이벤트를 적재하고 processed 처리한다")
    void should_append_stock_reservation_failed_event_when_reservation_fails_by_business_rule() throws Exception {
        EventEnvelope sourceEvent = stockReservationRequestedEvent();
        StockReservationRequestedPayload payload = stockReservationRequestedPayload();
        EventEnvelope failedEvent = stockReservationFailedEvent();

        mockClaimSuccess(sourceEvent);
        when(objectMapper.readValue(sourceEvent.payloadJson(), StockReservationRequestedPayload.class)).thenReturn(payload);
        when(stockReservationPort.findActiveReservationId(1L)).thenReturn(Optional.empty());
        when(productSagaEventFactory.stockReservationFailed(
                eq(sourceEvent),
                eq(payload),
                eq("STOCK_RESERVATION_FAILED"),
                anyString()
        )).thenReturn(failedEvent);
        Mockito.doThrow(new BusinessException(
                PaymentErrorCode.PAYMENT_STOCK_SHORTAGE,
                "재고가 부족합니다."
        )).when(stockReservationLocalTxService).reserve(anyString(), eq(1L), any());

        boolean handled = service.handle("product-group", "product-worker", 300L, sourceEvent);

        assertTrue(handled);
        verify(outboxEventPort).append(failedEvent);
        verify(processedEventPort).markProcessed(eq("product-group"), eq("event-1"), any(Instant.class));
        verify(processedEventPort, never()).releaseClaim(anyString(), anyString(), anyString());
    }

    /**
     * 결제 승인 이벤트는 기존 예약을 확정 상태로 바꿔야 한다.
     */
    @Test
    @DisplayName("PAYMENT_APPROVED를 받으면 예약을 CONFIRMED로 확정한다")
    void should_confirm_reservation_when_payment_approved_event_is_received() throws Exception {
        EventEnvelope approvedEvent = paymentApprovedEvent();
        PaymentApprovedPayload payload = new PaymentApprovedPayload(
                1L,
                10L,
                "reservation-1",
                "idem-1",
                "FAKE",
                "tx-1",
                new BigDecimal("10000"),
                "KRW"
        );

        mockClaimSuccess(approvedEvent);
        when(objectMapper.readValue(approvedEvent.payloadJson(), PaymentApprovedPayload.class)).thenReturn(payload);

        boolean handled = service.handle("product-group", "product-worker", 300L, approvedEvent);

        assertTrue(handled);
        verify(stockReservationPort).confirm("reservation-1");
        verify(processedEventPort).markProcessed(eq("product-group"), eq("event-2"), any(Instant.class));
        verify(outboxEventPort, never()).append(any());
    }

    /**
     * 결제 실패 이벤트는 기존 예약을 해제해야 한다.
     */
    @Test
    @DisplayName("PAYMENT_FAILED를 받으면 예약을 해제한다")
    void should_release_reservation_when_payment_failed_event_is_received() throws Exception {
        EventEnvelope failureEvent = paymentFailedEvent();
        PaymentFailedPayload payload = new PaymentFailedPayload(
                1L,
                "reservation-1",
                "idem-1",
                "FAKE",
                "PG_FAILED",
                "forced failure"
        );

        mockClaimSuccess(failureEvent);
        when(objectMapper.readValue(failureEvent.payloadJson(), PaymentFailedPayload.class)).thenReturn(payload);

        boolean handled = service.handle("product-group", "product-worker", 300L, failureEvent);

        assertTrue(handled);
        verify(stockReservationPort).release("reservation-1");
        verify(processedEventPort).markProcessed(eq("product-group"), eq("event-3"), any(Instant.class));
        verify(outboxEventPort, never()).append(any());
        verify(processedEventPort, never()).releaseClaim(anyString(), anyString(), anyString());
    }

    /**
     * 공통 claim 성공 상태를 세팅한다.
     */
    private void mockClaimSuccess(EventEnvelope envelope) {
        when(processedEventPort.claim(
                eq("product-group"),
                eq(envelope),
                eq("product-worker"),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(true);
    }

    private EventEnvelope stockReservationRequestedEvent() {
        return new EventEnvelope(
                "event-1",
                EventTypes.STOCK_RESERVATION_REQUESTED,
                "ORDER",
                "1",
                "saga-1",
                "corr-1",
                null,
                "order-service",
                "order.payment.saga.v1",
                "1",
                Instant.now(),
                "{\"orderId\":1}"
        );
    }

    private EventEnvelope paymentApprovedEvent() {
        return new EventEnvelope(
                "event-2",
                EventTypes.PAYMENT_APPROVED,
                "PAYMENT",
                "1",
                "saga-1",
                "corr-1",
                "event-1",
                "payment-service",
                "order.payment.saga.v1",
                "1",
                Instant.now(),
                "{\"orderId\":1}"
        );
    }

    private EventEnvelope paymentFailedEvent() {
        return new EventEnvelope(
                "event-3",
                EventTypes.PAYMENT_FAILED,
                "PAYMENT",
                "1",
                "saga-1",
                "corr-1",
                "event-2",
                "payment-service",
                "order.payment.saga.v1",
                "1",
                Instant.now(),
                "{\"orderId\":1}"
        );
    }

    private EventEnvelope stockReservedEvent() {
        return new EventEnvelope(
                "event-4",
                EventTypes.STOCK_RESERVED,
                "STOCK_RESERVATION",
                "reservation-1",
                "saga-1",
                "corr-1",
                "event-1",
                "product-service",
                "order.payment.saga.v1",
                "1",
                Instant.now(),
                "{\"orderId\":1}"
        );
    }

    private EventEnvelope stockReservationFailedEvent() {
        return new EventEnvelope(
                "event-5",
                EventTypes.STOCK_RESERVATION_FAILED,
                "STOCK_RESERVATION",
                "1",
                "saga-1",
                "corr-1",
                "event-1",
                "product-service",
                "order.payment.saga.v1",
                "1",
                Instant.now(),
                "{\"orderId\":1}"
        );
    }

    private StockReservationRequestedPayload stockReservationRequestedPayload() {
        return new StockReservationRequestedPayload(
                1L,
                1L,
                new BigDecimal("10000"),
                "KRW",
                "idem-1",
                "FAKE",
                List.of(new StockReservationItemPayload(10L, 2))
        );
    }
}
