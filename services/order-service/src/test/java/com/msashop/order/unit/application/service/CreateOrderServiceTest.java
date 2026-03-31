package com.msashop.order.unit.application.service;

import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.OrderErrorCode;
import com.msashop.order.application.port.in.model.CreateOrderCommand;
import com.msashop.order.application.port.out.LoadProductPort;
import com.msashop.order.application.port.out.OrderNumberPort;
import com.msashop.order.application.port.out.SaveOrderPort;
import com.msashop.order.application.port.out.SaveOrderStatusHistoryPort;
import com.msashop.order.application.port.out.model.ProductRow;
import com.msashop.order.application.service.CreateOrderService;
import com.msashop.order.domain.model.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {

    @Mock
    private OrderNumberPort orderNumberPort;

    @Mock
    private SaveOrderPort saveOrderPort;

    @Mock
    private SaveOrderStatusHistoryPort saveOrderStatusHistoryPort;

    @Mock
    private LoadProductPort loadProductPort;

    @InjectMocks
    private CreateOrderService service;

    @Test
    @DisplayName("상품이 일부 없으면 ORDER_PRODUCT_NOT_FOUND를 던진다")
    void should_throw_when_some_products_are_missing() {
        CreateOrderCommand command = OrderServiceFixtures.createOrderCommand();
        when(loadProductPort.loadProducts(List.of(10L))).thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createOrder(command));

        assertEquals(OrderErrorCode.ORDER_PRODUCT_NOT_FOUND, exception.errorCode());
        verify(saveOrderPort, never()).save(any());
        verify(saveOrderStatusHistoryPort, never()).saveHistory(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("비활성 상품이 포함되면 ORDER_PRODUCT_INACTIVE를 던진다")
    void should_throw_when_product_is_inactive() {
        CreateOrderCommand command = OrderServiceFixtures.createOrderCommand();
        ProductRow inactive = new ProductRow(10L, "테스트 상품", command.items().get(0).unitPrice(), 100, "ON_SALE", false);
        when(loadProductPort.loadProducts(List.of(10L))).thenReturn(List.of(inactive));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createOrder(command));

        assertEquals(OrderErrorCode.ORDER_PRODUCT_INACTIVE, exception.errorCode());
        verify(saveOrderPort, never()).save(any());
    }

    @Test
    @DisplayName("판매 중이 아닌 상품이 포함되면 ORDER_PRODUCT_NOT_ON_SALE을 던진다")
    void should_throw_when_product_is_not_on_sale() {
        CreateOrderCommand command = OrderServiceFixtures.createOrderCommand();
        ProductRow notOnSale = new ProductRow(10L, "테스트 상품", command.items().get(0).unitPrice(), 100, "SOLD_OUT", true);
        when(loadProductPort.loadProducts(List.of(10L))).thenReturn(List.of(notOnSale));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createOrder(command));

        assertEquals(OrderErrorCode.ORDER_PRODUCT_NOT_ON_SALE, exception.errorCode());
        verify(saveOrderPort, never()).save(any());
    }

    @Test
    @DisplayName("유효한 주문이면 CREATED 저장과 상태 이력을 남긴다")
    void should_create_order_and_save_history() {
        CreateOrderCommand command = OrderServiceFixtures.createOrderCommand();
        when(loadProductPort.loadProducts(List.of(10L))).thenReturn(List.of(OrderServiceFixtures.activeOnSaleProduct(10L)));
        when(orderNumberPort.nextOrderNumber()).thenReturn("ORD-001");
        when(saveOrderPort.save(any())).thenReturn(1L);

        Long orderId = service.createOrder(command);

        assertEquals(1L, orderId);
        verify(saveOrderPort).save(any());
        verify(saveOrderStatusHistoryPort).saveHistory(
                1L,
                null,
                OrderStatus.CREATED,
                "ORDER_CREATED",
                1L
        );
    }
}
