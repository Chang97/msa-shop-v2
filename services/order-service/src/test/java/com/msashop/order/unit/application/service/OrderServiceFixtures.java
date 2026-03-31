package com.msashop.order.unit.application.service;

import com.msashop.order.application.port.in.model.CreateOrderCommand;
import com.msashop.order.application.port.out.model.ProductRow;
import com.msashop.order.domain.model.Order;
import com.msashop.order.domain.model.OrderItem;
import com.msashop.order.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

final class OrderServiceFixtures {

    private OrderServiceFixtures() {
    }

    static Order order(OrderStatus status) {
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

    static CreateOrderCommand createOrderCommand() {
        return new CreateOrderCommand(
                1L,
                "KRW",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "홍길동",
                "010-1111-2222",
                "12345",
                "서울시 강남구",
                "101동 1001호",
                null,
                List.of(new CreateOrderCommand.CreateOrderItem(10L, "테스트 상품", new BigDecimal("10000"), 1))
        );
    }

    static ProductRow activeOnSaleProduct(Long productId) {
        return new ProductRow(
                productId,
                "테스트 상품",
                new BigDecimal("10000"),
                100,
                "ON_SALE",
                true
        );
    }
}
