package com.msashop.payment.application.service;

import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.payload.StockReservedPayload;
import com.msashop.payment.application.event.PaymentSagaEventFactory;
import com.msashop.payment.application.port.out.LoadPaymentPort;
import com.msashop.payment.application.port.out.OutboxEventPort;
import com.msashop.payment.application.port.out.ProcessedEventPort;
import com.msashop.payment.application.port.out.SavePaymentPort;
import com.msashop.payment.application.port.out.model.PaymentGatewayResult;
import com.msashop.payment.application.port.out.model.PaymentGatewayStatusResult;
import com.msashop.payment.domain.model.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PaymentSagaLocalTxService {

    private final LoadPaymentPort loadPaymentPort;
    private final SavePaymentPort savePaymentPort;
    private final ProcessedEventPort processedEventPort;
    private final OutboxEventPort outboxEventPort;
    private final PaymentSagaEventFactory paymentSagaEventFactory;

    @Transactional
    public PaymentTransaction findOrCreateRequested(EventEnvelope envelope, StockReservedPayload payload) {
        return loadPaymentPort.findByIdempotencyKey(payload.idempotencyKey())
                .orElseGet(() -> saveRequested(envelope, payload));
    }

    @Transactional
    public boolean completeIfAlreadyHandled(
            String consumerGroup,
            String eventId,
            EventEnvelope source,
            StockReservedPayload payload,
            PaymentTransaction payment
    ) {
        if (payment.isApproved()) {
            outboxEventPort.append(
                    paymentSagaEventFactory.paymentApproved(source, payload, payment)
            );
            processedEventPort.markProcessed(consumerGroup, eventId, Instant.now());
            return true;
        }

        if (payment.isFailed()) {
            outboxEventPort.append(
                    paymentSagaEventFactory.paymentFailed(
                            source,
                            payload,
                            "PAYMENT_ALREADY_FAILED",
                            payment.getFailReason()
                    )
            );
            processedEventPort.markProcessed(consumerGroup, eventId, Instant.now());
            return true;
        }

        if (payment.isApprovalUnknown()) {
            processedEventPort.markProcessed(consumerGroup, eventId, Instant.now());
            return true;
        }

        return false;
    }

    @Transactional
    public void approveAndMarkProcessed(
            String consumerGroup,
            String eventId,
            EventEnvelope source,
            StockReservedPayload payload,
            PaymentTransaction payment,
            PaymentGatewayResult gatewayResult
    ) {
        PaymentTransaction approved = savePaymentPort.save(
                payment.markApproved(gatewayResult.providerTxId(), Instant.now())
        );
        outboxEventPort.append(
                paymentSagaEventFactory.paymentApproved(source, payload, approved)
        );
        processedEventPort.markProcessed(consumerGroup, eventId, Instant.now());
    }

    @Transactional
    public void failAndMarkProcessed(
            String consumerGroup,
            String eventId,
            EventEnvelope source,
            StockReservedPayload payload,
            PaymentTransaction payment,
            PaymentGatewayResult gatewayResult
    ) {
        savePaymentPort.save(
                payment.markFailed(gatewayResult.reasonMessage(), Instant.now())
        );
        outboxEventPort.append(
                paymentSagaEventFactory.paymentFailed(
                        source,
                        payload,
                        gatewayResult.reasonCode(),
                        gatewayResult.reasonMessage()
                )
        );
        processedEventPort.markProcessed(consumerGroup, eventId, Instant.now());
    }

    @Transactional
    public void markApprovalUnknownAndProcessed(
            String consumerGroup,
            String eventId,
            PaymentTransaction payment,
            Exception gatewayException
    ) {
        savePaymentPort.save(
                payment.markApprovalUnknown(gatewayException.getMessage())
        );
        processedEventPort.markProcessed(consumerGroup, eventId, Instant.now());
    }

    @Transactional
    public void reconcileApproved(PaymentTransaction payment, PaymentGatewayStatusResult status) {
        PaymentTransaction approved = savePaymentPort.save(
                payment.markApproved(status.providerTxId(), Instant.now())
        );
        outboxEventPort.append(
                paymentSagaEventFactory.paymentApprovedByReconciliation(approved)
        );
    }

    @Transactional
    public void reconcileFailed(PaymentTransaction payment, PaymentGatewayStatusResult status) {
        PaymentTransaction failed = savePaymentPort.save(
                payment.markFailed(status.reasonMessage(), Instant.now())
        );
        outboxEventPort.append(
                paymentSagaEventFactory.paymentFailedByReconciliation(
                        failed,
                        status.reasonCode(),
                        status.reasonMessage()
                )
        );
    }

    private PaymentTransaction saveRequested(EventEnvelope envelope, StockReservedPayload payload) {
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
}
