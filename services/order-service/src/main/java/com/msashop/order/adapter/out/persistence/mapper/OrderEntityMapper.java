package com.msashop.order.adapter.out.persistence.mapper;

import com.msashop.order.adapter.out.persistence.entity.OrderEntity;
import com.msashop.order.adapter.out.persistence.entity.OrderItemEntity;
import com.msashop.order.domain.model.Order;
import com.msashop.order.domain.model.OrderItem;
import com.msashop.order.domain.model.OrderStatus;

import java.util.List;

public final class OrderEntityMapper {
    private OrderEntityMapper() {}

    public static Order toDomain(OrderEntity entity) {
        List<OrderItem> items = entity.getItems().stream()
                .map(i -> new OrderItem(
                        i.getProductId(),
                        i.getProductName(),
                        i.getUnitPrice(),
                        i.getQuantity()
                ))
                .toList();

        return Order.rehydrate(
                entity.getId(),
                entity.getOrderNumber(),
                entity.getUserId(),
                entity.getStatus(),
                entity.getCurrency(),
                entity.getSubtotalAmount(),
                entity.getDiscountAmount(),
                entity.getShippingFee(),
                entity.getTotalAmount(),
                entity.getReceiverName(),
                entity.getReceiverPhone(),
                entity.getShippingPostcode(),
                entity.getShippingAddress1(),
                entity.getShippingAddress2(),
                entity.getMemo(),
                items,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static OrderEntity toEntity(Order order) {
        OrderEntity entity = OrderEntity.builder()
                .id(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .status(order.getStatus())
                .currency(order.getCurrency())
                .subtotalAmount(order.getSubtotalAmount())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .shippingPostcode(order.getShippingPostcode())
                .shippingAddress1(order.getShippingAddress1())
                .shippingAddress2(order.getShippingAddress2())
                .memo(order.getMemo())
                .build();

        List<OrderItemEntity> itemEntities = order.getItems().stream()
                .map(i -> OrderItemEntity.builder()
                        .id(null)
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .unitPrice(i.getUnitPrice())
                        .quantity(i.getQuantity())
                        .lineAmount(i.getLineAmount())
                        .build())
                .toList();
        entity.setItems(itemEntities);
        return entity;
    }

    public static void applyStatus(OrderStatus status, OrderEntity entity) {
        entity.updateStatus(status);
    }
}

