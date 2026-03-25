package com.msashop.payment.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.InvalidSagaMessageException;
import com.msashop.common.event.SagaClaimExecutor;
import com.msashop.common.event.payload.StockReservedPayload;
import com.msashop.payment.application.event.PaymentSagaEventFactory;
import com.msashop.payment.application.port.in.HandleOrderPaymentRequestedUseCase;
import com.msashop.payment.application.port.out.LoadPaymentPort;
import com.msashop.payment.application.port.out.OutboxEventPort;
import com.msashop.payment.application.port.out.ProcessedEventPort;
import com.msashop.payment.application.port.out.RequestPaymentGatewayPort;
import com.msashop.payment.application.port.out.SavePaymentPort;
import com.msashop.payment.application.port.out.model.PaymentGatewayRequest;
import com.msashop.payment.application.port.out.model.PaymentGatewayResult;
import com.msashop.payment.domain.model.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderPaymentRequestedSagaService implements HandleOrderPaymentRequestedUseCase {

    private final ObjectMapper objectMapper;
    private final ProcessedEventPort processedEventPort;
    private final LoadPaymentPort loadPaymentPort;
    private final SavePaymentPort savePaymentPort;
    private final RequestPaymentGatewayPort requestPaymentGatewayPort;
    private final OutboxEventPort outboxEventPort;
    private final PaymentSagaEventFactory paymentSagaEventFactory;

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
                () -> handleClaimedEvent(consumerGroup, envelope),
                lastError -> processedEventPort.releaseClaim(consumerGroup, envelope.eventId(), lastError)
        );
    }

    private boolean handleClaimedEvent(String consumerGroup, EventEnvelope envelope) {
        // listener에서 이미 이벤트 타입을 걸러주지만,
        // 나중에 다른 진입점에서 이 use case를 재사용하더라도 안전하게 동작하도록 한 번 더 방어한다.
        if (!EventTypes.STOCK_RESERVED.equals(envelope.eventType())) {
            processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
            return true;
        }

        StockReservedPayload payload = deserializePayload(consumerGroup, envelope);

        Optional<PaymentTransaction> existing = loadPaymentPort.findByIdempotencyKey(payload.idempotencyKey());
        if (existing.isPresent()) {
            // 같은 메시지가 재전달돼도 PG를 다시 호출하면 안 된다.
            // 이미 저장된 최종 결제 결과를 재사용하고, 필요할 때만 하위 saga 이벤트를 다시 발행한다.
            publishExistingResult(envelope, payload, existing.get());
            processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
            return true;
        }

        // PG 호출 전에 REQUESTED 상태를 먼저 저장한다.
        // 그래야 PG는 결제를 승인했지만 우리 프로세스가 그 직후 죽은 경우에도
        // reconciliation이 이 row를 기준으로 복구할 수 있다.
        PaymentTransaction requestedPayment = saveRequestedPayment(envelope, payload);

        try {
            PaymentGatewayResult gatewayResult = requestPaymentGatewayPort.request(
                    new PaymentGatewayRequest(
                            payload.orderId(),
                            payload.userId(),
                            payload.amount(),
                            payload.currency(),
                            payload.idempotencyKey(),
                            payload.provider()
                    )
            );

            if (gatewayResult.approved()) {
                PaymentTransaction approvedPayment = saveApprovedPayment(requestedPayment, gatewayResult);
                outboxEventPort.append(
                        paymentSagaEventFactory.paymentApproved(envelope, payload, approvedPayment)
                );
                processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
                return true;
            }

            saveFailedPayment(requestedPayment, gatewayResult);
            outboxEventPort.append(
                    paymentSagaEventFactory.paymentFailed(
                            envelope,
                            payload,
                            gatewayResult.reasonCode(),
                            gatewayResult.reasonMessage()
                    )
            );
            processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
            return true;
        } catch (Exception gatewayException) {
            // timeout, 네트워크 단절 같은 애매한 PG 실패는 즉시 비즈니스 실패로 확정하지 않는다.
            // APPROVAL_UNKNOWN으로 남겨두고, 최종 판정은 reconciliation이 맡는다.
            saveApprovalUnknownPayment(requestedPayment, gatewayException);
            processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
            return true;
        }
    }

    private StockReservedPayload deserializePayload(String consumerGroup, EventEnvelope envelope) {
        try {
            return objectMapper.readValue(envelope.payloadJson(), StockReservedPayload.class);
        } catch (JsonProcessingException e) {
            processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
            throw new InvalidSagaMessageException(
                    "STOCK_RESERVED_PAYLOAD_DESERIALIZATION_FAILED",
                    "StockReserved payload deserialization failed.",
                    e
            );
        }
    }

    private PaymentTransaction saveRequestedPayment(EventEnvelope envelope, StockReservedPayload payload) {
        PaymentTransaction requested = PaymentTransaction.request(
                payload.orderId(),
                payload.userId(),
                payload.amount(),
                payload.currency(),
                payload.idempotencyKey(),
                payload.provider(),
                payload.reservationId(),
                envelope.sagaId(),
                envelope.correlationId(),
                envelope.eventId()
        );

        try {
            return savePaymentPort.save(requested);
        } catch (DataIntegrityViolationException e) {
            return loadPaymentPort.findByIdempotencyKey(payload.idempotencyKey()).orElseThrow(() -> e);
        }
    }

    private PaymentTransaction saveApprovedPayment(PaymentTransaction requestedPayment, PaymentGatewayResult gatewayResult) {
        return savePaymentPort.save(
                requestedPayment.markApproved(gatewayResult.providerTxId(), Instant.now())
        );
    }

    private PaymentTransaction saveFailedPayment(PaymentTransaction requestedPayment, PaymentGatewayResult gatewayResult) {
        return savePaymentPort.save(
                requestedPayment.markFailed(gatewayResult.reasonMessage(), Instant.now())
        );
    }

    private PaymentTransaction saveApprovalUnknownPayment(PaymentTransaction requestedPayment, Exception gatewayException) {
        return savePaymentPort.save(
                requestedPayment.markApprovalUnknown(gatewayException.getMessage())
        );
    }

    private void publishExistingResult(EventEnvelope source, StockReservedPayload payload, PaymentTransaction existing) {
        if (existing.isApproved()) {
            outboxEventPort.append(
                    paymentSagaEventFactory.paymentApproved(source, payload, existing)
            );
            return;
        }

        if (existing.isFailed()) {
            outboxEventPort.append(
                    paymentSagaEventFactory.paymentFailed(
                            source,
                            payload,
                            "PAYMENT_ALREADY_FAILED",
                            existing.getFailReason()
                    )
            );
            return;
        }

        // APPROVAL_UNKNOWN은 여기서 아무 이벤트도 발행하지 않는다.
        // 애매한 상태를 최종 결과로 바꾸는 책임은 reconciliation에만 둔다.
    }
}
