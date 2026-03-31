package com.msashop.order.application.service;

import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.OrderErrorCode;
import com.msashop.order.application.port.in.model.CancelOrderCommand;
import com.msashop.order.application.port.out.LoadOrderPort;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelOrderServiceTest {

    @Mock
    private LoadOrderPort loadOrderPort;

    @Mock
    private SaveOrderPort saveOrderPort;

    @Mock
    private SaveOrderStatusHistoryPort saveOrderStatusHistoryPort;

    @InjectMocks
    private CancelOrderService service;

    @Test
    @DisplayName("PAYMENT_EXPIRED 주문은 취소를 허용한다")
    void should_cancel_expired_order() {
        Order order = OrderServiceFixtures.order(OrderStatus.PAYMENT_EXPIRED);
        CancelOrderCommand command = new CancelOrderCommand(1L, 1L, "USER_CANCEL");
        when(loadOrderPort.loadOrder(1L)).thenReturn(order);

        service.cancelOrder(command);

        verify(saveOrderPort).save(order);
        verify(saveOrderStatusHistoryPort).saveHistory(
                1L,
                OrderStatus.PAYMENT_EXPIRED,
                OrderStatus.CANCELLED,
                "USER_CANCEL",
                1L
        );
    }

    @Test
    @DisplayName("PENDING_PAYMENT 주문은 취소할 수 없다")
    void should_throw_when_order_is_pending_payment() {
        Order order = OrderServiceFixtures.order(OrderStatus.PENDING_PAYMENT);
        CancelOrderCommand command = new CancelOrderCommand(1L, 1L, "USER_CANCEL");
        when(loadOrderPort.loadOrder(1L)).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.cancelOrder(command));

        assertEquals(OrderErrorCode.ORDER_CANCEL_NOT_ALLOWED, exception.errorCode());
        verify(saveOrderPort, never()).save(any());
        verify(saveOrderStatusHistoryPort, never()).saveHistory(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("이미 CANCELLED인 주문은 저장이나 이력을 남기지 않는다")
    void should_not_save_when_order_is_already_cancelled() {
        Order order = OrderServiceFixtures.order(OrderStatus.CANCELLED);
        CancelOrderCommand command = new CancelOrderCommand(1L, 1L, "USER_CANCEL");
        when(loadOrderPort.loadOrder(1L)).thenReturn(order);

        service.cancelOrder(command);

        verify(saveOrderPort, never()).save(any());
        verify(saveOrderStatusHistoryPort, never()).saveHistory(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("PAID 주문은 취소할 수 없다")
    void should_throw_when_order_is_paid() {
        Order order = OrderServiceFixtures.order(OrderStatus.PAID);
        CancelOrderCommand command = new CancelOrderCommand(1L, 1L, "USER_CANCEL");
        when(loadOrderPort.loadOrder(1L)).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.cancelOrder(command));

        assertEquals(OrderErrorCode.ORDER_CANCEL_NOT_ALLOWED, exception.errorCode());
        verify(saveOrderPort, never()).save(any());
    }
}
