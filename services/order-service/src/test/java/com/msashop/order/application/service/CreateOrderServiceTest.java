package com.msashop.order.application.service;

import com.msashop.common.web.exception.ConflictException;
import com.msashop.order.application.port.in.model.CreateOrderCommand;
import com.msashop.order.application.port.out.LoadProductPort;
import com.msashop.order.application.port.out.OrderNumberPort;
import com.msashop.order.application.port.out.SaveOrderPort;
import com.msashop.order.application.port.out.SaveOrderStatusHistoryPort;
import com.msashop.order.application.port.out.model.ProductRow;
import com.msashop.order.domain.model.Order;
import com.msashop.order.domain.model.OrderItem;
import com.msashop.order.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    private CreateOrderService createOrderService;

    @Test
    void createsOrderUsingProductSnapshot() {
        CreateOrderCommand command = createCommand(2);
        ProductRow product = new ProductRow(10L, "DB상품", new BigDecimal("5000"), 5, "ON_SALE", true);

        when(orderNumberPort.nextOrderNumber()).thenReturn("ORD-001");
        when(loadProductPort.loadProducts(any())).thenReturn(List.of(product));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        when(saveOrderPort.save(orderCaptor.capture())).thenReturn(99L);

        Long orderId = createOrderService.createOrder(command);

        assertThat(orderId).isEqualTo(99L);
        Order saved = orderCaptor.getValue();
        assertThat(saved.getItems()).hasSize(1);
        OrderItem item = saved.getItems().get(0);
        assertThat(item.getProductName()).isEqualTo("DB상품");
        assertThat(item.getUnitPrice()).isEqualByComparingTo("5000");
        assertThat(item.getQuantity()).isEqualTo(2);
        verify(saveOrderStatusHistoryPort).saveHistory(99L, null, OrderStatus.CREATED, "ORDER_CREATED", command.userId());
    }

    @Test
    void throwsWhenStockInsufficient() {
        CreateOrderCommand command = createCommand(3);
        ProductRow product = new ProductRow(10L, "DB상품", new BigDecimal("5000"), 2, "ON_SALE", true);

        when(loadProductPort.loadProducts(any())).thenReturn(List.of(product));

        assertThatThrownBy(() -> createOrderService.createOrder(command))
                .isInstanceOf(ConflictException.class);

        verifyNoInteractions(orderNumberPort);
        verify(saveOrderPort, never()).save(any());
    }

    @Test
    void throwsWhenProductNotOnSale() {
        CreateOrderCommand command = createCommand(1);
        ProductRow product = new ProductRow(10L, "DB상품", new BigDecimal("5000"), 5, "STOPPED", true);

        when(loadProductPort.loadProducts(any())).thenReturn(List.of(product));

        assertThatThrownBy(() -> createOrderService.createOrder(command))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(orderNumberPort);
        verify(saveOrderPort, never()).save(any());
    }

    @Test
    void validatesAggregatedQuantityForDuplicateLines() {
        CreateOrderCommand command = new CreateOrderCommand(
                1L,
                "KRW",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "수취인",
                "010-0000-0000",
                "12345",
                "주소1",
                "주소2",
                "메모",
                List.of(
                        new CreateOrderCommand.CreateOrderItem(10L, "임의명1", new BigDecimal("1000"), 3),
                        new CreateOrderCommand.CreateOrderItem(10L, "임의명2", new BigDecimal("2000"), 3)
                )
        );
        ProductRow product = new ProductRow(10L, "DB상품", new BigDecimal("5000"), 5, "ON_SALE", true);

        when(loadProductPort.loadProducts(any())).thenReturn(List.of(product));

        assertThatThrownBy(() -> createOrderService.createOrder(command))
                .isInstanceOf(ConflictException.class);
    }

    private CreateOrderCommand createCommand(int quantity) {
        return new CreateOrderCommand(
                1L,
                "KRW",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "수취인",
                "010-0000-0000",
                "12345",
                "주소1",
                "주소2",
                "메모",
                List.of(new CreateOrderCommand.CreateOrderItem(10L, "클라이언트전송명", new BigDecimal("12345"), quantity))
        );
    }
}
