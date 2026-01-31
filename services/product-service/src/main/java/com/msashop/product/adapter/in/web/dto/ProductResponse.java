package com.msashop.product.adapter.in.web.dto;

import java.math.BigDecimal;
import com.msashop.product.domain.model.ProductStatus;

public record ProductResponse(
        Long productId,
        String productName,
        BigDecimal price,
        int stock,
        ProductStatus status,
        Boolean useYn
) {
}
