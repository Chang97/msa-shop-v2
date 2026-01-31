package com.msashop.order.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank String currency,
        @NotNull BigDecimal discountAmount,
        @NotNull BigDecimal shippingFee,
        @NotBlank @Size(max = 80) String receiverName,
        @NotBlank @Size(max = 32) String receiverPhone,
        @NotBlank @Size(max = 10) String shippingPostcode,
        @NotBlank @Size(max = 255) String shippingAddress1,
        @Size(max = 255) String shippingAddress2,
        @Size(max = 255) String memo,
        @NotEmpty List<@Valid OrderItemRequest> items
) {
    public record OrderItemRequest(
            @NotNull Long productId,
            @NotBlank String productName,
            @NotNull BigDecimal unitPrice,
            @Positive int quantity
    ) {}
}

