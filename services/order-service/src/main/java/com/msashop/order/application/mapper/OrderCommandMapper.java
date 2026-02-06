package com.msashop.order.application.mapper;

import com.msashop.order.application.port.in.model.CreateOrderCommand;
import com.msashop.order.application.port.out.model.ProductRow;
import com.msashop.order.domain.model.Order;
import com.msashop.order.domain.model.OrderItem;

import java.util.List;
import java.util.Map;

public final class OrderCommandMapper {
    private OrderCommandMapper() {}

    public static Order toDomain(String orderNumber, CreateOrderCommand command, Map<Long, ProductRow> productsById) {
        List<OrderItem> items = command.items().stream()
                .map(i -> {
                    ProductRow product = productsById.get(i.productId());
                    return new OrderItem(product.productId(), product.productName(), product.price(), i.quantity());
                })
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
