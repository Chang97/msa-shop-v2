package com.msashop.order.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MarkPaidRequest(
        @NotNull Long paymentId,
        @NotBlank String idempotencyKey,
        String reason
) {}

