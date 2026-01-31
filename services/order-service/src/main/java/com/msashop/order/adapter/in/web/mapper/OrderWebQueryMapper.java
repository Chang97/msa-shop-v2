package com.msashop.order.adapter.in.web.mapper;

import com.msashop.order.adapter.in.web.dto.OrderItemResponse;
import com.msashop.order.adapter.in.web.dto.OrderResponse;
import com.msashop.order.application.port.in.model.OrderResult;

import java.util.List;

public final class OrderWebQueryMapper {
    private OrderWebQueryMapper() {}

    public static OrderResponse toResponse(OrderResult result) {
        List<OrderItemResponse> items = result.items().stream()
                .map(i -> new OrderItemResponse(
                        i.productId(),
                        i.productName(),
                        i.unitPrice(),
                        i.quantity(),
                        i.lineAmount()
                ))
                .toList();
        return new OrderResponse(
                result.orderId(),
                result.orderNumber(),
                result.status(),
                result.currency(),
                result.subtotalAmount(),
                result.discountAmount(),
                result.shippingFee(),
                result.totalAmount(),
                result.receiverName(),
                result.receiverPhone(),
                result.shippingPostcode(),
                result.shippingAddress1(),
                result.shippingAddress2(),
                result.memo(),
                items,
                result.createdAt(),
                result.updatedAt()
        );
    }
}

