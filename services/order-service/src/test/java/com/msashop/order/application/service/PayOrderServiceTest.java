package com.msashop.order.application.service;

import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.OrderErrorCode;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.order.application.event.OrderPaymentSagaEventFactory;
import com.msashop.order.application.port.in.model.PayOrderCommand;
import com.msashop.order.application.port.out.LoadOrderPort;
import com.msashop.order.application.port.out.OutboxEventPort;
import com.msashop.order.application.port.out.SaveOrderPort;
import com.msashop.order.application.port.out.SaveOrderStatusHistoryPort;
import com.msashop.order.domain.model.Order;
import com.msashop.order.domain.model.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    @DisplayName("CREATED 주문은 결제 시작 시 saga를 시작한다")
    void should_start_payment_and_append_stock_reservation_requested_event() {
        Order order = OrderServiceFixtures.order(OrderStatus.CREATED);
        PayOrderCommand command = new PayOrderCommand(1L, 1L, "idem-1", "FAKE");
        EventEnvelope sagaStartEvent = sagaStartEvent();

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

    @Test
    @DisplayName("PAYMENT_FAILED 주문은 재결제를 허용한다")
    void should_restart_payment_when_order_is_payment_failed() {
        Order order = OrderServiceFixtures.order(OrderStatus.PAYMENT_FAILED);
        PayOrderCommand command = new PayOrderCommand(1L, 1L, "idem-1", "FAKE");
        EventEnvelope sagaStartEvent = sagaStartEvent();

        when(loadOrderPort.loadOrder(1L)).thenReturn(order);
        when(orderPaymentSagaEventFactory.stockReservationRequested(order, command)).thenReturn(sagaStartEvent);

        service.payOrder(command);

        verify(saveOrderPort).save(order);
        verify(saveOrderStatusHistoryPort).saveHistory(
                1L,
                OrderStatus.PAYMENT_FAILED,
                OrderStatus.PENDING_PAYMENT,
                "PAYMENT_STARTED",
                1L
        );
        verify(outboxEventPort).append(sagaStartEvent);
    }

    @Test
    @DisplayName("PAYMENT_EXPIRED 주문은 재결제를 허용한다")
    void should_restart_payment_when_order_is_payment_expired() {
        Order order = OrderServiceFixtures.order(OrderStatus.PAYMENT_EXPIRED);
        PayOrderCommand command = new PayOrderCommand(1L, 1L, "idem-1", "FAKE");
        EventEnvelope sagaStartEvent = sagaStartEvent();

        when(loadOrderPort.loadOrder(1L)).thenReturn(order);
        when(orderPaymentSagaEventFactory.stockReservationRequested(order, command)).thenReturn(sagaStartEvent);

        service.payOrder(command);

        verify(saveOrderPort).save(order);
        verify(saveOrderStatusHistoryPort).saveHistory(
                1L,
                OrderStatus.PAYMENT_EXPIRED,
                OrderStatus.PENDING_PAYMENT,
                "PAYMENT_STARTED",
                1L
        );
        verify(outboxEventPort).append(sagaStartEvent);
    }

    @Test
    @DisplayName("PENDING_PAYMENT 주문은 saga를 다시 시작하지 않는다")
    void should_not_append_event_when_order_is_already_pending_payment() {
        Order order = OrderServiceFixtures.order(OrderStatus.PENDING_PAYMENT);
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

    @Test
    @DisplayName("다른 사용자의 주문은 결제를 시작할 수 없다")
    void should_throw_when_user_does_not_own_order() {
        Order order = OrderServiceFixtures.order(OrderStatus.CREATED);
        when(loadOrderPort.loadOrder(1L)).thenReturn(order);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.payOrder(new PayOrderCommand(1L, 999L, "idem-1", "FAKE"))
        );

        assertEquals(OrderErrorCode.ORDER_ACCESS_DENIED, exception.errorCode());
        verify(saveOrderPort, never()).save(any());
    }

    private EventEnvelope sagaStartEvent() {
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
}
