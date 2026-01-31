package com.msashop.product.application.port.in.model;

import java.math.BigDecimal;

public record CreateProductCommand(
        String productName,
        BigDecimal price,
        int stock
) {
}
