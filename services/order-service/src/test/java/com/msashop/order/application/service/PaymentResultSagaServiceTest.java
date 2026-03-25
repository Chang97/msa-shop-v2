package com.msashop.order.application.service;

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
import com.msashop.order.domain.model.Order;
import com.msashop.order.domain.model.OrderItem;
import com.msashop.order.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PaymentResultSagaService 단위 테스트.
 *
 * 검증 목적:
 * - order-service가 결제 saga 결과를 어떻게 주문 상태에 반영하는지 검증한다
 *
 * 핵심 정책:
 * - PaymentApproved면 주문을 PAID로 전이
 * - PaymentFailed면 주문 상태는 유지하고 history만 기록
 * - StockReservationFailed도 주문 상태는 유지하고 history만 기록
 */
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

    /**
     * 결제 승인 완료 케이스.
     *
     * 시나리오:
     * - 주문 상태가 PENDING_PAYMENT
     * - PaymentApproved 이벤트 수신
     *
     * 기대값:
     * - 주문 상태가 PAID로 전이된다
     * - 주문 저장이 호출된다
     * - PAYMENT_APPROVED history가 남는다
     * - 원본 이벤트는 processed 처리된다
     */
    @Test
    void should_mark_order_paid_when_payment_approved_event_is_received() throws Exception {
        EventEnvelope approvedEvent = new EventEnvelope(
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

        PaymentApprovedPayload payload = new PaymentApprovedPayload(
                1L,
                100L,
                "reservation-1",
                "idem-1",
                "FAKE",
                "pg-tx-1",
                new BigDecimal("10000"),
                "KRW"
        );

        Order order = createOrder(OrderStatus.PENDING_PAYMENT);

        when(processedEventPort.claim(
                eq("order-group"),
                eq(approvedEvent),
                eq("order-worker"),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(true);
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

    /**
     * 결제 실패 케이스.
     *
     * 시나리오:
     * - PaymentFailed 이벤트 수신
     *
     * 기대값:
     * - 주문 상태는 PENDING_PAYMENT 그대로 유지된다
     * - 별도 상태 전이 없이 PAYMENT_FAILED history만 남는다
     * - 주문 저장은 호출되지 않는다
     * - 원본 이벤트는 processed 처리된다
     */
    @Test
    void should_keep_pending_payment_and_only_write_history_when_payment_failed_event_is_received() throws Exception {
        EventEnvelope failedEvent = new EventEnvelope(
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

        PaymentFailedPayload payload = new PaymentFailedPayload(
                1L,
                "reservation-1",
                "idem-1",
                "FAKE",
                "PG_FAILED",
                "forced failure"
        );

        Order order = createOrder(OrderStatus.PENDING_PAYMENT);

        when(processedEventPort.claim(
                eq("order-group"),
                eq(failedEvent),
                eq("order-worker"),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(true);
        when(objectMapper.readValue(failedEvent.payloadJson(), PaymentFailedPayload.class)).thenReturn(payload);
        when(loadOrderPort.loadOrder(1L)).thenReturn(order);

        boolean handled = service.handle("order-group", "order-worker", 300L, failedEvent);

        assertTrue(handled);
        verify(saveOrderPort, never()).save(any());
        verify(saveOrderStatusHistoryPort).saveHistory(
                1L,
                OrderStatus.PENDING_PAYMENT,
                OrderStatus.PENDING_PAYMENT,
                "PAYMENT_FAILED:PG_FAILED",
                1L
        );
        verify(processedEventPort).markProcessed(eq("order-group"), eq("event-2"), any(Instant.class));
        verify(processedEventPort, never()).releaseClaim(any(), any(), any());
    }

    /**
     * 재고 예약 실패 케이스.
     *
     * 시나리오:
     * - StockReservationFailed 이벤트 수신
     *
     * 기대값:
     * - 주문 상태는 그대로 유지된다
     * - STOCK_RESERVATION_FAILED history만 남는다
     * - 주문 저장은 호출되지 않는다
     * - 원본 이벤트는 processed 처리된다
     *
     * 의미:
     * - 이 케이스는 PG를 호출하기 전에 흐름이 멈춘 경우다
     */
    @Test
    void should_keep_pending_payment_and_only_write_history_when_stock_reservation_failed_event_is_received() throws Exception {
        EventEnvelope failedEvent = new EventEnvelope(
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

        StockReservationFailedPayload payload = new StockReservationFailedPayload(
                1L,
                "idem-1",
                "STOCK_RESERVATION_FAILED",
                "insufficient stock"
        );

        Order order = createOrder(OrderStatus.PENDING_PAYMENT);

        when(processedEventPort.claim(
                eq("order-group"),
                eq(failedEvent),
                eq("order-worker"),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(true);
        when(objectMapper.readValue(failedEvent.payloadJson(), StockReservationFailedPayload.class)).thenReturn(payload);
        when(loadOrderPort.loadOrder(1L)).thenReturn(order);

        boolean handled = service.handle("order-group", "order-worker", 300L, failedEvent);

        assertTrue(handled);
        verify(saveOrderPort, never()).save(any());
        verify(saveOrderStatusHistoryPort).saveHistory(
                1L,
                OrderStatus.PENDING_PAYMENT,
                OrderStatus.PENDING_PAYMENT,
                "STOCK_RESERVATION_FAILED:STOCK_RESERVATION_FAILED",
                1L
        );
        verify(processedEventPort).markProcessed(eq("order-group"), eq("event-3"), any(Instant.class));
        verify(processedEventPort, never()).releaseClaim(any(), any(), any());
    }

    private Order createOrder(OrderStatus status) {
        return Order.rehydrate(
                1L,
                "ORD-001",
                1L,
                status,
                "KRW",
                new BigDecimal("10000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("10000"),
                "홍길동",
                "010-1111-2222",
                "12345",
                "서울시 강남구",
                "101동 1001호",
                null,
                List.of(new OrderItem(10L, "테스트 상품", new BigDecimal("10000"), 1)),
                Instant.now(),
                Instant.now()
        );
    }
}