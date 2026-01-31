package com.msashop.order.adapter.in.web.mapper;

import com.msashop.order.adapter.in.web.dto.CreateOrderRequest;
import com.msashop.order.application.port.in.model.CreateOrderCommand;

import java.util.List;

public final class OrderWebCommandMapper {
    private OrderWebCommandMapper() {}

    public static CreateOrderCommand toCommand(Long userId, CreateOrderRequest request) {
        List<CreateOrderCommand.CreateOrderItem> items = request.items().stream()
                .map(i -> new CreateOrderCommand.CreateOrderItem(
                        i.productId(),
                        i.productName(),
                        i.unitPrice(),
                        i.quantity()
                ))
                .toList();

        return new CreateOrderCommand(
                userId,
                request.currency(),
                request.discountAmount(),
                request.shippingFee(),
                request.receiverName(),
                request.receiverPhone(),
                request.shippingPostcode(),
                request.shippingAddress1(),
                request.shippingAddress2(),
                request.memo(),
                items
        );
    }
}

