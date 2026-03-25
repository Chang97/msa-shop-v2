package com.msashop.payment.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTopics;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.payload.PaymentApprovedPayload;
import com.msashop.common.event.payload.PaymentFailedPayload;
import com.msashop.common.event.payload.StockReservedPayload;
import com.msashop.payment.domain.model.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * payment-service가 결제 처리 결과를 다시 saga 토픽으로 내보낼 때 사용하는 factory.
 * source 이벤트의 saga/correlation/causation 정보를 이어받아
 * 체인 추적이 가능하도록 맞춘다.
 */
@Component
@RequiredArgsConstructor
public class PaymentSagaEventFactory {
    private final ObjectMapper objectMapper;

    public EventEnvelope paymentApproved(EventEnvelope source,
                                         StockReservedPayload sourcePayload,
                                         PaymentTransaction payment) {
        return new EventEnvelope(
                UUID.randomUUID().toString(),
                EventTypes.PAYMENT_APPROVED,
                "PAYMENT",
                String.valueOf(payment.getPaymentId()),
                source.sagaId(),
                source.correlationId(),
                source.eventId(),
                "payment-service",
                EventTopics.ORDER_PAYMENT_SAGA_V1,
                String.valueOf(sourcePayload.orderId()),
                Instant.now(),
                writeJson(new PaymentApprovedPayload(
                        sourcePayload.orderId(),
                        payment.getPaymentId(),
                        sourcePayload.reservationId(),
                        sourcePayload.idempotencyKey(),
                        sourcePayload.provider(),
                        payment.getProviderTxId(),
                        payment.getAmount(),
                        payment.getCurrency()
                ))
        );
    }

    public EventEnvelope paymentFailed(EventEnvelope source,
                                       StockReservedPayload sourcePayload,
                                       String reasonCode,
                                       String reasonMessage) {
        return new EventEnvelope(
                UUID.randomUUID().toString(),
                EventTypes.PAYMENT_FAILED,
                "PAYMENT",
                String.valueOf(sourcePayload.orderId()),
                source.sagaId(),
                source.correlationId(),
                source.eventId(),
                "payment-service",
                EventTopics.ORDER_PAYMENT_SAGA_V1,
                String.valueOf(sourcePayload.orderId()),
                Instant.now(),
                writeJson(new PaymentFailedPayload(
                        sourcePayload.orderId(),
                        sourcePayload.reservationId(),
                        sourcePayload.idempotencyKey(),
                        sourcePayload.provider(),
                        reasonCode,
                        reasonMessage
                ))
        );
    }

    public EventEnvelope paymentApprovedByReconciliation(PaymentTransaction payment) {
        return new EventEnvelope(
                UUID.randomUUID().toString(),
                EventTypes.PAYMENT_APPROVED,
                "PAYMENT",
                String.valueOf(payment.getPaymentId()),
                payment.getSagaId(),
                payment.getCorrelationId(),
                payment.getSourceEventId(),
                "payment-service",
                EventTopics.ORDER_PAYMENT_SAGA_V1,
                String.valueOf(payment.getOrderId()),
                Instant.now(),
                writeJson(new PaymentApprovedPayload(
                        payment.getOrderId(),
                        payment.getPaymentId(),
                        payment.getReservationId(),
                        payment.getIdempotencyKey(),
                        payment.getProvider(),
                        payment.getProviderTxId(),
                        payment.getAmount(),
                        payment.getCurrency()
                ))
        );
    }

    public EventEnvelope paymentFailedByReconciliation(
            PaymentTransaction payment,
            String reasonCode,
            String reasonMessage
    ) {
        return new EventEnvelope(
                UUID.randomUUID().toString(),
                EventTypes.PAYMENT_FAILED,
                "PAYMENT",
                String.valueOf(payment.getPaymentId()),
                payment.getSagaId(),
                payment.getCorrelationId(),
                payment.getSourceEventId(),
                "payment-service",
                EventTopics.ORDER_PAYMENT_SAGA_V1,
                String.valueOf(payment.getOrderId()),
                Instant.now(),
                writeJson(new PaymentFailedPayload(
                        payment.getOrderId(),
                        payment.getReservationId(),
                        payment.getIdempotencyKey(),
                        payment.getProvider(),
                        reasonCode,
                        reasonMessage
                ))
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("결제 saga 이벤트 payload 직렬화에 실패했습니다.", e);
        }
    }
}
