package com.msashop.order.adapter.in.web.dto;

import com.msashop.order.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long orderId,
        String orderNumber,
        OrderStatus status,
        String currency,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        BigDecimal totalAmount,
        String receiverName,
        String receiverPhone,
        String shippingPostcode,
        String shippingAddress1,
        String shippingAddress2,
        String memo,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {}

