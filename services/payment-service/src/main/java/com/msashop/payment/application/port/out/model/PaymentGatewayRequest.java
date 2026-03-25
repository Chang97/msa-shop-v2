package com.msashop.payment.application.port.out.model;

import java.math.BigDecimal;

public record PaymentGatewayRequest(
        Long orderId,
        Long userId,
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        String provider
) {
}