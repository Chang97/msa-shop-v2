package com.msashop.product.application.port.in.model;

import java.math.BigDecimal;
import com.msashop.product.domain.model.ProductStatus;

public record ProductResult(
        Long productId,
        String productName,
        BigDecimal price,
        int stock,
        ProductStatus status,
        Boolean useYn
) {
}
