package com.msashop.order.application.mapper;

import com.msashop.order.application.port.in.model.OrderItemResult;
import com.msashop.order.application.port.in.model.OrderResult;
import com.msashop.order.domain.model.Order;

import java.util.List;

public final class OrderQueryMapper {
    private OrderQueryMapper() {}

    public static OrderResult toResult(Order order) {
        List<OrderItemResult> items = order.getItems().stream()
                .map(i -> new OrderItemResult(
                        i.getProductId(),
                        i.getProductName(),
                        i.getUnitPrice(),
                        i.getQuantity(),
                        i.getLineAmount()
                ))
                .toList();
        return new OrderResult(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getUserId(),
                order.getStatus(),
                order.getCurrency(),
                order.getSubtotalAmount(),
                order.getDiscountAmount(),
                order.getShippingFee(),
                order.getTotalAmount(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getShippingPostcode(),
                order.getShippingAddress1(),
                order.getShippingAddress2(),
                order.getMemo(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}

