package com.msashop.payment.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.InvalidSagaMessageException;
import com.msashop.common.event.SagaClaimExecutor;
import com.msashop.common.event.payload.StockReservedPayload;
import com.msashop.payment.application.port.in.HandleOrderPaymentRequestedUseCase;
import com.msashop.payment.application.port.out.LoadPaymentPort;
import com.msashop.payment.application.port.out.ProcessedEventPort;
import com.msashop.payment.application.port.out.RequestPaymentGatewayPort;
import com.msashop.payment.application.port.out.model.PaymentGatewayRequest;
import com.msashop.payment.application.port.out.model.PaymentGatewayResult;
import com.msashop.payment.domain.model.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderPaymentRequestedSagaService implements HandleOrderPaymentRequestedUseCase {

    private final ObjectMapper objectMapper;
    private final ProcessedEventPort processedEventPort;
    private final LoadPaymentPort loadPaymentPort;
    private final RequestPaymentGatewayPort requestPaymentGatewayPort;
    private final PaymentSagaLocalTxService paymentSagaLocalTxService;

    @Override
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
                () -> handleClaimedEvent(consumerGroup, envelope),
                lastError -> processedEventPort.releaseClaim(consumerGroup, envelope.eventId(), lastError)
        );
    }

    private boolean handleClaimedEvent(String consumerGroup, EventEnvelope envelope) {
        if (!EventTypes.STOCK_RESERVED.equals(envelope.eventType())) {
            processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
            return true;
        }

        StockReservedPayload payload = deserializePayload(consumerGroup, envelope);

        Optional<PaymentTransaction> existing = loadPaymentPort.findByIdempotencyKey(payload.idempotencyKey());
        if (existing.isPresent() && paymentSagaLocalTxService.completeIfAlreadyHandled(
                consumerGroup,
                envelope.eventId(),
                envelope,
                payload,
                existing.get()
        )) {
            return true;
        }

        PaymentTransaction requestedPayment = paymentSagaLocalTxService.findOrCreateRequested(envelope, payload);

        PaymentGatewayResult gatewayResult;
        try {
            gatewayResult = requestPaymentGatewayPort.request(
                    new PaymentGatewayRequest(
                            payload.orderId(),
                            payload.userId(),
                            payload.amount(),
                            payload.currency(),
                            payload.idempotencyKey(),
                            payload.provider()
                    )
            );
        } catch (Exception gatewayException) {
            paymentSagaLocalTxService.markApprovalUnknownAndProcessed(
                    consumerGroup,
                    envelope.eventId(),
                    requestedPayment,
                    gatewayException
            );
            return true;
        }

        if (gatewayResult.approved()) {
            paymentSagaLocalTxService.approveAndMarkProcessed(
                    consumerGroup,
                    envelope.eventId(),
                    envelope,
                    payload,
                    requestedPayment,
                    gatewayResult
            );
            return true;
        }

        paymentSagaLocalTxService.failAndMarkProcessed(
                consumerGroup,
                envelope.eventId(),
                envelope,
                payload,
                requestedPayment,
                gatewayResult
        );
        return true;
    }

    private StockReservedPayload deserializePayload(String consumerGroup, EventEnvelope envelope) {
        try {
            return objectMapper.readValue(envelope.payloadJson(), StockReservedPayload.class);
        } catch (JsonProcessingException e) {
            processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
            throw new InvalidSagaMessageException(
                    "STOCK_RESERVED_PAYLOAD_DESERIALIZATION_FAILED",
                    "StockReserved payload 역직렬화에 실패했습니다.",
                    e
            );
        }
    }
}
