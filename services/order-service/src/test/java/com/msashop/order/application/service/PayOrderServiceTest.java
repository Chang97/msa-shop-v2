package com.msashop.order.application.service;

import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.order.application.event.OrderPaymentSagaEventFactory;
import com.msashop.order.application.port.in.model.PayOrderCommand;
import com.msashop.order.application.port.out.LoadOrderPort;
import com.msashop.order.application.port.out.OutboxEventPort;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PayOrderService 단위 테스트.
 *
 * 검증 목적:
 * - order-service가 결제 시작 가능 상태를 올바르게 판단하는지
 * - 주문 상태 변경, history 저장, saga 시작 이벤트 적재를 기대한 조건에서만 수행하는지
 */
@ExtendWith(MockitoExtension.class)
class PayOrderServiceTest {

    @Mock
    private LoadOrderPort loadOrderPort;

    @Mock
    private SaveOrderPort saveOrderPort;

    @Mock
    private SaveOrderStatusHistoryPort saveOrderStatusHistoryPort;

    @Mock
    private OutboxEventPort outboxEventPort;

    @Mock
    private OrderPaymentSagaEventFactory orderPaymentSagaEventFactory;

    @InjectMocks
    private PayOrderService service;

    /**
     * 정상 결제 시작 케이스.
     *
     * 시나리오:
     * - 주문 상태가 CREATED
     * - 사용자가 pay 요청
     *
     * 기대값:
     * - 주문 상태가 PENDING_PAYMENT로 전이된다
     * - 주문 저장이 호출된다
     * - 상태 history가 CREATED -> PENDING_PAYMENT로 기록된다
     * - StockReservationRequested saga 시작 이벤트가 outbox에 적재된다
     */
    @Test
    void should_start_payment_and_append_stock_reservation_requested_event() {
        Order order = createOrder(OrderStatus.CREATED);
        PayOrderCommand command = new PayOrderCommand(1L, 1L, "idem-1", "FAKE");

        EventEnvelope sagaStartEvent = new EventEnvelope(
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

        when(loadOrderPort.loadOrder(1L)).thenReturn(order);
        when(orderPaymentSagaEventFactory.stockReservationRequested(order, command)).thenReturn(sagaStartEvent);

        service.payOrder(command);

        verify(saveOrderPort).save(order);
        verify(saveOrderStatusHistoryPort).saveHistory(
                1L,
                OrderStatus.CREATED,
                OrderStatus.PENDING_PAYMENT,
                "PAYMENT_STARTED",
                1L
        );
        verify(outboxEventPort).append(sagaStartEvent);
    }

    /**
     * 중복 결제 시작 요청 방지 케이스.
     *
     * 시나리오:
     * - 주문 상태가 이미 PENDING_PAYMENT
     * - 사용자가 pay를 다시 요청
     *
     * 기대값:
     * - 주문 저장이 다시 일어나지 않는다
     * - 상태 history를 다시 남기지 않는다
     * - saga 시작 이벤트를 중복 발행하지 않는다
     */
    @Test
    void should_not_append_event_when_order_is_already_pending_payment() {
        Order order = createOrder(OrderStatus.PENDING_PAYMENT);
        PayOrderCommand command = new PayOrderCommand(1L, 1L, "idem-1", "FAKE");

        when(loadOrderPort.loadOrder(1L)).thenReturn(order);

        service.payOrder(command);

        verify(saveOrderPort, never()).save(order);
        verify(saveOrderStatusHistoryPort, never()).saveHistory(
                eq(1L),
                eq(OrderStatus.PENDING_PAYMENT),
                eq(OrderStatus.PENDING_PAYMENT),
                eq("PAYMENT_STARTED"),
                eq(1L)
        );
        verify(outboxEventPort, never()).append(any());
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
