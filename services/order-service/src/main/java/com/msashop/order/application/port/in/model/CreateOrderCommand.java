package com.msashop.order.application.port.in.model;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderCommand(
        Long userId,
        String currency,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        String receiverName,
        String receiverPhone,
        String shippingPostcode,
        String shippingAddress1,
        String shippingAddress2,
        String memo,
        List<CreateOrderItem> items
) {
    public record CreateOrderItem(
            Long productId,
            String productName,
            BigDecimal unitPrice,
            int quantity
    ) {}
}

