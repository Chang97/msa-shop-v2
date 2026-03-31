package com.msashop.order.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.InvalidSagaMessageException;
import com.msashop.common.event.SagaClaimExecutor;
import com.msashop.common.event.payload.PaymentApprovedPayload;
import com.msashop.common.event.payload.PaymentFailedPayload;
import com.msashop.common.event.payload.StockReservationFailedPayload;
import com.msashop.order.application.port.in.HandlePaymentResultUseCase;
import com.msashop.order.application.port.out.LoadOrderPort;
import com.msashop.order.application.port.out.ProcessedEventPort;
import com.msashop.order.application.port.out.SaveOrderPort;
import com.msashop.order.application.port.out.SaveOrderStatusHistoryPort;
import com.msashop.order.domain.model.Order;
import com.msashop.order.domain.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PaymentResultSagaService implements HandlePaymentResultUseCase {

    private final ObjectMapper objectMapper;
    private final ProcessedEventPort processedEventPort;
    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;
    private final SaveOrderStatusHistoryPort saveOrderStatusHistoryPort;

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
                    case EventTypes.PAYMENT_APPROVED -> handlePaymentApproved(consumerGroup, envelope);
                    case EventTypes.PAYMENT_FAILED -> handlePaymentFailed(consumerGroup, envelope);
                    case EventTypes.STOCK_RESERVATION_FAILED -> handleStockReservationFailed(consumerGroup, envelope);
                    default -> {
                        processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
                        yield true;
                    }
                },
                lastError -> processedEventPort.releaseClaim(consumerGroup, envelope.eventId(), lastError)
        );
    }

    private boolean handlePaymentApproved(String consumerGroup, EventEnvelope envelope) {
        PaymentApprovedPayload payload = deserializeApprovedPayload(consumerGroup, envelope);
        Order order = loadOrderPort.loadOrder(payload.orderId());
        OrderStatus from = order.getStatus();

        order.markPaid();

        if (from != order.getStatus()) {
            saveOrderPort.save(order);
            saveOrderStatusHistoryPort.saveHistory(
                    order.getOrderId(),
                    from,
                    order.getStatus(),
                    historyReason(from, "PAYMENT_APPROVED"),
                    order.getUserId()
            );
        }

        processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
        return true;
    }

    private boolean handlePaymentFailed(String consumerGroup, EventEnvelope envelope) {
        PaymentFailedPayload payload = deserializeFailedPayload(consumerGroup, envelope);
        Order order = loadOrderPort.loadOrder(payload.orderId());
        OrderStatus from = order.getStatus();

        order.markPaymentFailed();

        if (from != order.getStatus()) {
            saveOrderPort.save(order);
        }
        saveOrderStatusHistoryPort.saveHistory(
                order.getOrderId(),
                from,
                order.getStatus(),
                historyReason(from, "PAYMENT_FAILED") + ":" + payload.reasonCode(),
                order.getUserId()
        );

        processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
        return true;
    }

    private boolean handleStockReservationFailed(String consumerGroup, EventEnvelope envelope) {
        StockReservationFailedPayload payload = deserializeReservationFailedPayload(consumerGroup, envelope);
        Order order = loadOrderPort.loadOrder(payload.orderId());
        OrderStatus from = order.getStatus();

        order.markPaymentFailed();

        if (from != order.getStatus()) {
            saveOrderPort.save(order);
        }
        saveOrderStatusHistoryPort.saveHistory(
                order.getOrderId(),
                from,
                order.getStatus(),
                historyReason(from, "STOCK_RESERVATION_FAILED") + ":" + payload.reasonCode(),
                order.getUserId()
        );

        processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
        return true;
    }

    private PaymentApprovedPayload deserializeApprovedPayload(String consumerGroup, EventEnvelope envelope) {
        try {
            return objectMapper.readValue(envelope.payloadJson(), PaymentApprovedPayload.class);
        } catch (JsonProcessingException e) {
            processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
            throw new InvalidSagaMessageException(
                    "PAYMENT_APPROVED_PAYLOAD_DESERIALIZATION_FAILED",
                    "PaymentApproved payload 역직렬화에 실패했습니다.",
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
                    "PaymentFailed payload 역직렬화에 실패했습니다.",
                    e
            );
        }
    }

    private StockReservationFailedPayload deserializeReservationFailedPayload(String consumerGroup, EventEnvelope envelope) {
        try {
            return objectMapper.readValue(envelope.payloadJson(), StockReservationFailedPayload.class);
        } catch (JsonProcessingException e) {
            processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
            throw new InvalidSagaMessageException(
                    "STOCK_RESERVATION_FAILED_PAYLOAD_DESERIALIZATION_FAILED",
                    "StockReservationFailed payload 역직렬화에 실패했습니다.",
                    e
            );
        }
    }

    private String historyReason(OrderStatus from, String baseReason) {
        if (from == OrderStatus.PAYMENT_EXPIRED) {
            return baseReason + "_LATE";
        }
        return baseReason;
    }
}
