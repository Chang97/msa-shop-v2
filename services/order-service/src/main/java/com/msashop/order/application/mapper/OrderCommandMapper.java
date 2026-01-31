package com.msashop.order.application.mapper;

import com.msashop.order.application.port.in.model.CreateOrderCommand;
import com.msashop.order.domain.model.Order;
import com.msashop.order.domain.model.OrderItem;

import java.util.List;

public final class OrderCommandMapper {
    private OrderCommandMapper() {}

    public static Order toDomain(String orderNumber, CreateOrderCommand command) {
        List<OrderItem> items = command.items().stream()
                .map(i -> new OrderItem(i.productId(), i.productName(), i.unitPrice(), i.quantity()))
                .toList();
        return Order.create(
                orderNumber,
                command.userId(),
                command.currency(),
                command.discountAmount(),
                command.shippingFee(),
                command.receiverName(),
                command.receiverPhone(),
                command.shippingPostcode(),
                command.shippingAddress1(),
                command.shippingAddress2(),
                command.memo(),
                items
        );
    }
}

