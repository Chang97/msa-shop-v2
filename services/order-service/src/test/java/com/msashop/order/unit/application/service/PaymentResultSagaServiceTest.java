package com.msashop.order.unit.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.payload.PaymentApprovedPayload;
import com.msashop.common.event.payload.PaymentFailedPayload;
import com.msashop.common.event.payload.StockReservationFailedPayload;
import com.msashop.order.application.port.out.LoadOrderPort;
import com.msashop.order.application.port.out.ProcessedEventPort;
import com.msashop.order.application.port.out.SaveOrderPort;
import com.msashop.order.application.port.out.SaveOrderStatusHistoryPort;
import com.msashop.order.application.service.PaymentResultSagaService;
import com.msashop.order.domain.model.Order;
import com.msashop.order.domain.model.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentResultSagaServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProcessedEventPort processedEventPort;

    @Mock
    private LoadOrderPort loadOrderPort;

    @Mock
    private SaveOrderPort saveOrderPort;

    @Mock
    private SaveOrderStatusHistoryPort saveOrderStatusHistoryPort;

    @InjectMocks
    private PaymentResultSagaService service;

    @Test
    @DisplayName("PAYMENT_APPROVED면 주문을 PAID로 전이한다")
    void should_mark_order_paid_when_payment_approved_event_is_received() throws Exception {
        EventEnvelope approvedEvent = approvedEvent();
        PaymentApprovedPayload payload = approvedPayload();
        Order order = OrderServiceFixtures.order(OrderStatus.PENDING_PAYMENT);

        mockClaimSuccess(approvedEvent);
        when(objectMapper.readValue(approvedEvent.payloadJson(), PaymentApprovedPayload.class)).thenReturn(payload);
        when(loadOrderPort.loadOrder(1L)).thenReturn(order);

        boolean handled = service.handle("order-group", "order-worker", 300L, approvedEvent);

        assertTrue(handled);
        verify(saveOrderPort).save(order);
        verify(saveOrderStatusHistoryPort).saveHistory(
                1L,
                OrderStatus.PENDING_PAYMENT,
                OrderStatus.PAID,
                "PAYMENT_APPROVED",
                1L
        );
        verify(processedEventPort).markProcessed(eq("order-group"), eq("event-1"), any(Instant.class));
        verify(processedEventPort, never()).releaseClaim(any(), any(), any());
    }

    @Test
    @DisplayName("PAYMENT_FAILED면 주문을 PAYMENT_FAILED로 전이한다")
    void should_mark_order_payment_failed_when_payment_failed_event_is_received() throws Exception {
        EventEnvelope failedEvent = paymentFailedEvent();
        PaymentFailedPayload payload = paymentFailedPayload();
        Order order = OrderServiceFixtures.order(OrderStatus.PENDING_PAYMENT);

        mockClaimSuccess(failedEvent);
        when(objectMapper.readValue(failedEvent.payloadJson(), PaymentFailedPayload.class)).thenReturn(payload);
        when(loadOrderPort.loadOrder(1L)).thenReturn(order);

        boolean handled = service.handle("order-group", "order-worker", 300L, failedEvent);

        assertTrue(handled);
        verify(saveOrderPort).save(order);
        verify(saveOrderStatusHistoryPort).saveHistory(
                1L,
                OrderStatus.PENDING_PAYMENT,
                OrderStatus.PAYMENT_FAILED,
                "PAYMENT_FAILED:PG_FAILED",
                1L
        );
        verify(processedEventPort).markProcessed(eq("order-group"), eq("event-2"), any(Instant.class));
    }

    @Test
    @DisplayName("STOCK_RESERVATION_FAILED면 주문을 PAYMENT_FAILED로 전이한다")
    void should_mark_order_payment_failed_when_stock_reservation_failed_event_is_received() throws Exception {
        EventEnvelope failedEvent = stockReservationFailedEvent();
        StockReservationFailedPayload payload = stockReservationFailedPayload();
        Order order = OrderServiceFixtures.order(OrderStatus.PENDING_PAYMENT);

        mockClaimSuccess(failedEvent);
        when(objectMapper.readValue(failedEvent.payloadJson(), StockReservationFailedPayload.class)).thenReturn(payload);
        when(loadOrderPort.loadOrder(1L)).thenReturn(order);

        boolean handled = service.handle("order-group", "order-worker", 300L, failedEvent);

        assertTrue(handled);
        verify(saveOrderPort).save(order);
        verify(saveOrderStatusHistoryPort).saveHistory(
                1L,
                OrderStatus.PENDING_PAYMENT,
                OrderStatus.PAYMENT_FAILED,
                "STOCK_RESERVATION_FAILED:STOCK_RESERVATION_FAILED",
                1L
        );
        verify(processedEventPort).markProcessed(eq("order-group"), eq("event-3"), any(Instant.class));
    }

    @Test
    @DisplayName("PAYMENT_EXPIRED 이후 늦게 도착한 PAYMENT_APPROVED도 PAID로 반영한다")
    void should_mark_order_paid_when_payment_approved_arrives_late() throws Exception {
        EventEnvelope approvedEvent = approvedEvent();
        PaymentApprovedPayload payload = approvedPayload();
        Order order = OrderServiceFixtures.order(OrderStatus.PAYMENT_EXPIRED);

        mockClaimSuccess(approvedEvent);
        when(objectMapper.readValue(approvedEvent.payloadJson(), PaymentApprovedPayload.class)).thenReturn(payload);
        when(loadOrderPort.loadOrder(1L)).thenReturn(order);

        boolean handled = service.handle("order-group", "order-worker", 300L, approvedEvent);

        assertTrue(handled);
        verify(saveOrderPort).save(order);
        verify(saveOrderStatusHistoryPort).saveHistory(
                1L,
                OrderStatus.PAYMENT_EXPIRED,
                OrderStatus.PAID,
                "PAYMENT_APPROVED_LATE",
                1L
        );
    }

    @Test
    @DisplayName("PAYMENT_EXPIRED 이후 늦게 도착한 PAYMENT_FAILED도 PAYMENT_FAILED로 반영한다")
    void should_mark_order_payment_failed_when_payment_failed_arrives_late() throws Exception {
        EventEnvelope failedEvent = paymentFailedEvent();
        PaymentFailedPayload payload = paymentFailedPayload();
        Order order = OrderServiceFixtures.order(OrderStatus.PAYMENT_EXPIRED);

        mockClaimSuccess(failedEvent);
        when(objectMapper.readValue(failedEvent.payloadJson(), PaymentFailedPayload.class)).thenReturn(payload);
        when(loadOrderPort.loadOrder(1L)).thenReturn(order);

        boolean handled = service.handle("order-group", "order-worker", 300L, failedEvent);

        assertTrue(handled);
        verify(saveOrderPort).save(order);
        verify(saveOrderStatusHistoryPort).saveHistory(
                1L,
                OrderStatus.PAYMENT_EXPIRED,
                OrderStatus.PAYMENT_FAILED,
                "PAYMENT_FAILED_LATE:PG_FAILED",
                1L
        );
    }

    @Test
    @DisplayName("PAYMENT_EXPIRED 이후 늦게 도착한 STOCK_RESERVATION_FAILED도 PAYMENT_FAILED로 반영한다")
    void should_mark_order_payment_failed_when_stock_reservation_failed_arrives_late() throws Exception {
        EventEnvelope failedEvent = stockReservationFailedEvent();
        StockReservationFailedPayload payload = stockReservationFailedPayload();
        Order order = OrderServiceFixtures.order(OrderStatus.PAYMENT_EXPIRED);

        mockClaimSuccess(failedEvent);
        when(objectMapper.readValue(failedEvent.payloadJson(), StockReservationFailedPayload.class)).thenReturn(payload);
        when(loadOrderPort.loadOrder(1L)).thenReturn(order);

        boolean handled = service.handle("order-group", "order-worker", 300L, failedEvent);

        assertTrue(handled);
        verify(saveOrderPort).save(order);
        verify(saveOrderStatusHistoryPort).saveHistory(
                1L,
                OrderStatus.PAYMENT_EXPIRED,
                OrderStatus.PAYMENT_FAILED,
                "STOCK_RESERVATION_FAILED_LATE:STOCK_RESERVATION_FAILED",
                1L
        );
    }

    private void mockClaimSuccess(EventEnvelope envelope) {
        when(processedEventPort.claim(
                eq("order-group"),
                eq(envelope),
                eq("order-worker"),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(true);
    }

    private EventEnvelope approvedEvent() {
        return new EventEnvelope(
                "event-1",
                EventTypes.PAYMENT_APPROVED,
                "PAYMENT",
                "100",
                "saga-1",
                "corr-1",
                "cause-1",
                "payment-service",
                "order.payment.saga.v1",
                "1",
                Instant.now(),
                "{\"orderId\":1}"
        );
    }

    private EventEnvelope paymentFailedEvent() {
        return new EventEnvelope(
                "event-2",
                EventTypes.PAYMENT_FAILED,
                "PAYMENT",
                "100",
                "saga-1",
                "corr-1",
                "cause-1",
                "payment-service",
                "order.payment.saga.v1",
                "1",
                Instant.now(),
                "{\"orderId\":1}"
        );
    }

    private EventEnvelope stockReservationFailedEvent() {
        return new EventEnvelope(
                "event-3",
                EventTypes.STOCK_RESERVATION_FAILED,
                "STOCK_RESERVATION",
                "1",
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

    private PaymentApprovedPayload approvedPayload() {
        return new PaymentApprovedPayload(
                1L,
                100L,
                "reservation-1",
                "idem-1",
                "FAKE",
                "pg-tx-1",
                new BigDecimal("10000"),
                "KRW"
        );
    }

    private PaymentFailedPayload paymentFailedPayload() {
        return new PaymentFailedPayload(
                1L,
                "reservation-1",
                "idem-1",
                "FAKE",
                "PG_FAILED",
                "forced failure"
        );
    }

    private StockReservationFailedPayload stockReservationFailedPayload() {
        return new StockReservationFailedPayload(
                1L,
                "idem-1",
                "STOCK_RESERVATION_FAILED",
                "insufficient stock"
        );
    }
}
