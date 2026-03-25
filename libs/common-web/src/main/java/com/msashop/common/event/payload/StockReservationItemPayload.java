package com.msashop.common.event.payload;

public record StockReservationItemPayload(
        Long productId,
        Integer quantity
) {
}