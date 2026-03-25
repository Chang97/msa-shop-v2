package com.msashop.order.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTopics;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.payload.StockReservationItemPayload;
import com.msashop.common.event.payload.StockReservationRequestedPayload;
import com.msashop.order.application.port.in.model.PayOrderCommand;
import com.msashop.order.domain.model.Order;
import com.msashop.order.domain.model.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * order-service가 결제 시작 시 발행하는 saga 이벤트를 생성한다.
 * 이번 단계에서는 결제 요청을 바로 payment로 넘기지 않고,
 * 먼저 product-service가 재고를 예약할 수 있도록 StockReservationRequested를 만든다.
 */
@Component
@RequiredArgsConstructor
public class OrderPaymentSagaEventFactory {
    private final ObjectMapper objectMapper;

    /**
     * 결제 시작 시점의 주문 정보를 재고 예약 요청 이벤트로 변환한다.
     *
     * eventId:
     * 현재 이벤트 자체의 고유 식별자
     *
     * sagaId:
     * 이번 주문 결제 saga 전체를 추적하는 식별자
     *
     * correlationId:
     * saga 시작 이벤트이므로 자기 자신의 eventId를 correlationId로 둔다.
     *
     * causationId:
     * 시작 이벤트라서 없다.
     */
    public EventEnvelope stockReservationRequested(Order order, PayOrderCommand command) {
        String eventId = UUID.randomUUID().toString();
        String sagaId = UUID.randomUUID().toString();

        StockReservationRequestedPayload payload = new StockReservationRequestedPayload(
                order.getOrderId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getCurrency(),
                command.idempotencyKey(),
                command.provider(),
                toReservationItems(order.getItems())
        );

        return new EventEnvelope(
                eventId,
                EventTypes.STOCK_RESERVATION_REQUESTED,
                "ORDER",
                String.valueOf(order.getOrderId()),
                sagaId,
                eventId,
                null,
                "order-service",
                EventTopics.ORDER_PAYMENT_SAGA_V1,
                String.valueOf(order.getOrderId()),
                Instant.now(),
                writeJson(payload)
        );
    }

    private List<StockReservationItemPayload> toReservationItems(List<OrderItem> items) {
        return items.stream()
                .map(item -> new StockReservationItemPayload(
                        item.getProductId(),
                        item.getQuantity()
                ))
                .toList();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("이벤트 payload 직렬화에 실패했습니다.", e);
        }
    }
}
