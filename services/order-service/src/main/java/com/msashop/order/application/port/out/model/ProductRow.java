package com.msashop.order.application.port.out.model;

import java.math.BigDecimal;

public record ProductRow(
        Long productId,
        String productName,
        BigDecimal price,
        Integer stock,
        String status,
        Boolean useYn
) {
}
