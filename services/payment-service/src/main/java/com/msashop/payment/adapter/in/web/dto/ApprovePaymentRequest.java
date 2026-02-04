package com.msashop.payment.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ApprovePaymentRequest(
        @NotNull Long orderId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
        @NotBlank String idempotencyKey
) {}

