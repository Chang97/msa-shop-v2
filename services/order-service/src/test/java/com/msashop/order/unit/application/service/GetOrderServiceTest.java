package com.msashop.order.unit.application.service;

import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.OrderErrorCode;
import com.msashop.order.application.port.out.LoadOrderPort;
import com.msashop.order.application.service.GetOrderService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetOrderServiceTest {

    @Mock
    private LoadOrderPort loadOrderPort;

    @InjectMocks
    private GetOrderService service;

    @Test
    @DisplayName("주문 소유자는 단건 조회할 수 있다")
    void should_return_order_for_owner() {
        Order order = OrderServiceFixtures.order(OrderStatus.CREATED);
        when(loadOrderPort.loadOrder(1L)).thenReturn(order);

        var result = service.getOrder(1L, 1L);

        assertEquals(1L, result.orderId());
        assertEquals(OrderStatus.CREATED, result.status());
    }

    @Test
    @DisplayName("다른 사용자의 주문은 단건 조회할 수 없다")
    void should_throw_when_user_is_not_owner() {
        Order order = OrderServiceFixtures.order(OrderStatus.CREATED);
        when(loadOrderPort.loadOrder(1L)).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.getOrder(1L, 2L));

        assertEquals(OrderErrorCode.ORDER_ACCESS_DENIED, exception.errorCode());
    }
}
