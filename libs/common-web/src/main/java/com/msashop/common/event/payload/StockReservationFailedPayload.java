package com.msashop.common.event.payload;

public record StockReservationFailedPayload(
        Long orderId,
        String idempotencyKey,
        String reasonCode,
        String reasonMessage
) {
}