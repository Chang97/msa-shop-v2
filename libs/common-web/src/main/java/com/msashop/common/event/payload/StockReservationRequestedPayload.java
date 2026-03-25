package com.msashop.common.event.payload;

import java.math.BigDecimal;
import java.util.List;

/**
 * order-service가 product-service에 재고 예약을 요청할 때 보내는 payload.
 * payment-service가 이후 결제 요청을 만들 수 있도록
 * userId, amount, currency도 처음부터 함께 들고 간다.
 */
public record StockReservationRequestedPayload(
        Long orderId,
        Long userId,
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        String provider,
        List<StockReservationItemPayload> items
) {
}