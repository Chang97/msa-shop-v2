package com.msashop.common.event.payload;

import java.math.BigDecimal;

/**
 * product-service가 재고 예약 성공 후 payment-service로 넘길 payload.
 * payment-service가 별도 order 조회 없이 바로 PG 요청을 만들 수 있어야 하므로
 * 결제에 필요한 정보(userId, amount, currency)를 그대로 실어 보낸다.
 */
public record StockReservedPayload(
        Long orderId,
        Long userId,
        String reservationId,
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        String provider
) {
}