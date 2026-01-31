package com.msashop.product.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotNull @Size(max = 120) String productName,
        BigDecimal price,
        int stock
) {
}
