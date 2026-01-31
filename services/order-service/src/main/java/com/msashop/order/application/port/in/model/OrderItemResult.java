package com.msashop.order.application.port.in.model;

import java.math.BigDecimal;

public record OrderItemResult(
        Long productId,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineAmount
) {}

