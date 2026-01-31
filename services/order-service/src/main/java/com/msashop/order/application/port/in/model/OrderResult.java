package com.msashop.order.application.port.in.model;

import com.msashop.order.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResult(
        Long orderId,
        String orderNumber,
        Long userId,
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
        List<OrderItemResult> items,
        Instant createdAt,
        Instant updatedAt
) {}

