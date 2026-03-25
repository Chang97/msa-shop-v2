package com.msashop.product.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.InvalidSagaMessageException;
import com.msashop.common.event.SagaClaimExecutor;
import com.msashop.common.event.payload.PaymentApprovedPayload;
import com.msashop.common.event.payload.PaymentFailedPayload;
import com.msashop.common.event.payload.StockReservationRequestedPayload;
import com.msashop.common.web.exception.ConflictException;
import com.msashop.product.application.event.ProductSagaEventFactory;
import com.msashop.product.application.port.in.HandleOrderPaymentSagaUseCase;
import com.msashop.product.application.port.out.OutboxEventPort;
import com.msashop.product.application.port.out.ProcessedEventPort;
import com.msashop.product.application.port.out.StockReservationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderPaymentSagaService implements HandleOrderPaymentSagaUseCase {

    private final ObjectMapper objectMapper;
    private final ProcessedEventPort processedEventPort;
    private final StockReservationPort stockReservationPort;
    private final OutboxEventPort outboxEventPort;
    private final ProductSagaEventFactory productSagaEventFactory;

    @Override
    @Transactional
    public boolean handle(
            String consumerGroup,
            String workerId,
            long claimTimeoutSeconds,
            EventEnvelope envelope
    ) throws Exception {
        return SagaClaimExecutor.execute(
                claimTimeoutSeconds,
                (now, staleThreshold) -> processedEventPort.claim(
                        consumerGroup,
                        envelope,
                        workerId,
                        now,
                        staleThreshold
                ),
                () -> switch (envelope.eventType()) {
                    case EventTypes.STOCK_RESERVATION_REQUESTED -> handleReservationRequested(consumerGroup, envelope);
                    case EventTypes.PAYMENT_APPROVED -> handlePaymentApproved(consumerGroup, envelope);
                    case EventTypes.PAYMENT_FAILED -> handlePaymentFailed(consumerGroup, envelope);
                    default -> {
                        processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
                        yield true;
                    }
                },
                lastError -> processedEventPort.releaseClaim(consumerGroup, envelope.eventId(), lastError)
        );
    }

    private boolean handleReservationRequested(String consumerGroup, EventEnvelope envelope) {
        StockReservationRequestedPayload payload = deserializeRequestedPayload(consumerGroup, envelope);

        try {
            String reservationId = stockReservationPort.findActiveReservationId(payload.orderId())
                    .orElseGet(() -> {
                        // 예약 id는 product-service가 발급한다.
                        // 재고 예약/확정/해제의 진실 소스가 product-service이기 때문이다.
                        String newReservationId = UUID.randomUUID().toString();
                        stockReservationPort.reserve(newReservationId, payload.orderId(), payload.items());
                        return newReservationId;
                    });

            outboxEventPort.append(
                    productSagaEventFactory.stockReserved(envelope, payload, reservationId)
            );
        } catch (ConflictException e) {
            outboxEventPort.append(
                    productSagaEventFactory.stockReservationFailed(
                            envelope,
                            payload,
                            "STOCK_RESERVATION_FAILED",
                            e.getMessage()
                    )
            );
        }

        processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
        return true;
    }

    private boolean handlePaymentApproved(String consumerGroup, EventEnvelope envelope) {
        PaymentApprovedPayload payload = deserializeApprovedPayload(consumerGroup, envelope);

        // 재고는 예약 시점에 이미 감소했다.
        // 결제 승인 시점에는 그 예약을 최종 확정만 한다.
        stockReservationPort.confirm(payload.reservationId());
        processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
        return true;
    }

    private boolean handlePaymentFailed(String consumerGroup, EventEnvelope envelope) {
        PaymentFailedPayload payload = deserializeFailedPayload(consumerGroup, envelope);

        // 결제 실패 시에는 예약을 해제해서 판매 가능한 재고를 복구해야 한다.
        stockReservationPort.release(payload.reservationId());
        processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
        return true;
    }

    private StockReservationRequestedPayload deserializeRequestedPayload(String consumerGroup, EventEnvelope envelope) {
        try {
            return objectMapper.readValue(envelope.payloadJson(), StockReservationRequestedPayload.class);
        } catch (JsonProcessingException e) {
            processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
            throw new InvalidSagaMessageException(
                    "STOCK_RESERVATION_REQUESTED_PAYLOAD_DESERIALIZATION_FAILED",
                    "StockReservationRequested payload deserialization failed.",
                    e
            );
        }
    }

    private PaymentApprovedPayload deserializeApprovedPayload(String consumerGroup, EventEnvelope envelope) {
        try {
            return objectMapper.readValue(envelope.payloadJson(), PaymentApprovedPayload.class);
        } catch (JsonProcessingException e) {
            processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
            throw new InvalidSagaMessageException(
                    "PAYMENT_APPROVED_PAYLOAD_DESERIALIZATION_FAILED",
                    "PaymentApproved payload deserialization failed.",
                    e
            );
        }
    }

    private PaymentFailedPayload deserializeFailedPayload(String consumerGroup, EventEnvelope envelope) {
        try {
            return objectMapper.readValue(envelope.payloadJson(), PaymentFailedPayload.class);
        } catch (JsonProcessingException e) {
            processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
            throw new InvalidSagaMessageException(
                    "PAYMENT_FAILED_PAYLOAD_DESERIALIZATION_FAILED",
                    "PaymentFailed payload deserialization failed.",
                    e
            );
        }
    }
}
