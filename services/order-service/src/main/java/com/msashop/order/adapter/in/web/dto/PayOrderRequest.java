package com.msashop.order.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record PayOrderRequest(
        @NotBlank String idempotencyKey,
        @NotBlank String provider
) {
}