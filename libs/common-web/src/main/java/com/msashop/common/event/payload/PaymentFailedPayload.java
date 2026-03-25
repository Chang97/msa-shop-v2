package com.msashop.common.event.payload;

public record PaymentFailedPayload(
        Long orderId,
        String reservationId,
        String idempotencyKey,
        String provider,
        String reasonCode,
        String reasonMessage
) {
}