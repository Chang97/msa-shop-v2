package com.msashop.product.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTopics;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.payload.StockReservationFailedPayload;
import com.msashop.common.event.payload.StockReservationRequestedPayload;
import com.msashop.common.event.payload.StockReservedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * product-service가 예약 성공/실패 결과를 saga 토픽으로 다시 발행할 때 사용하는 factory.
 */
@Component
@RequiredArgsConstructor
public class ProductSagaEventFactory {

    private final ObjectMapper objectMapper;

    public EventEnvelope stockReserved(
            EventEnvelope source,
            StockReservationRequestedPayload payload,
            String reservationId
    ) {
        return new EventEnvelope(
                UUID.randomUUID().toString(),
                EventTypes.STOCK_RESERVED,
                "STOCK_RESERVATION",
                reservationId,
                source.sagaId(),
                source.correlationId(),
                source.eventId(),
                "product-service",
                EventTopics.ORDER_PAYMENT_SAGA_V1,
                String.valueOf(payload.orderId()),
                Instant.now(),
                writeJson(new StockReservedPayload(
                        payload.orderId(),
                        payload.userId(),
                        reservationId,
                        payload.amount(),
                        payload.currency(),
                        payload.idempotencyKey(),
                        payload.provider()
                ))
        );
    }

    public EventEnvelope stockReservationFailed(
            EventEnvelope source,
            StockReservationRequestedPayload payload,
            String reasonCode,
            String reasonMessage
    ) {
        return new EventEnvelope(
                UUID.randomUUID().toString(),
                EventTypes.STOCK_RESERVATION_FAILED,
                "STOCK_RESERVATION",
                String.valueOf(payload.orderId()),
                source.sagaId(),
                source.correlationId(),
                source.eventId(),
                "product-service",
                EventTopics.ORDER_PAYMENT_SAGA_V1,
                String.valueOf(payload.orderId()),
                Instant.now(),
                writeJson(new StockReservationFailedPayload(
                        payload.orderId(),
                        payload.idempotencyKey(),
                        reasonCode,
                        reasonMessage
                ))
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("product saga payload 직렬화에 실패했습니다.", e);
        }
    }
}