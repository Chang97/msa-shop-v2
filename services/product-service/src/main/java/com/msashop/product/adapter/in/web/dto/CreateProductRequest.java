package com.msashop.product.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotNull(message = "상품명은 필수입니다.")
        @Size(max = 120, message = "상품명은 120자 이하여야 합니다.")
        String productName,
        BigDecimal price,
        int stock
) {
}
