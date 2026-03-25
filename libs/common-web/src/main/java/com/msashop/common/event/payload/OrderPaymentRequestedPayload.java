package com.msashop.common.event.payload;

import java.math.BigDecimal;

public record OrderPaymentRequestedPayload(
        Long orderId,
        Long userId,
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        String provider
) {
}