package com.msashop.common.event.payload;

import java.math.BigDecimal;

public record PaymentApprovedPayload(
        Long orderId,
        Long paymentId,
        String reservationId,
        String idempotencyKey,
        String provider,
        String providerTxId,
        BigDecimal amount,
        String currency
) {
}