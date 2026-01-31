package com.msashop.order.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelOrderRequest(@NotBlank String reason) {
}

