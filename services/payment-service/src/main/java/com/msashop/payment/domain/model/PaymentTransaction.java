package com.msashop.payment.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public class PaymentTransaction {

    private final Long paymentId;
    private final Long orderId;
    private final Long userId;
    private final BigDecimal amount;
    private final String currency;
    private final String idempotencyKey;
    private final String provider;
    private final String providerTxId;
    private final String reservationId;
    private final String sagaId;
    private final String correlationId;
    private final String sourceEventId;
    private final PaymentStatus status;
    private final Instant requestedAt;
    private final Instant approvedAt;
    private final Instant failedAt;
    private final String failReason;
    private final Instant createdAt;
    private final Instant updatedAt;

    private PaymentTransaction(Long paymentId,
                               Long orderId,
                               Long userId,
                               BigDecimal amount,
                               String currency,
                               String idempotencyKey,
                               String provider,
                               String providerTxId,
                               String reservationId,
                               String sagaId,
                               String correlationId,
                               String sourceEventId,
                               PaymentStatus status,
                               Instant requestedAt,
                               Instant approvedAt,
                               Instant failedAt,
                               String failReason,
                               Instant createdAt,
                               Instant updatedAt) {
        this.paymentId = paymentId;
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.providerTxId = providerTxId;
        this.reservationId = reservationId;
        this.sagaId = sagaId;
        this.correlationId = correlationId;
        this.sourceEventId = sourceEventId;
        this.status = Objects.requireNonNull(status, "status");
        this.requestedAt = requestedAt;
        this.approvedAt = approvedAt;
        this.failedAt = failedAt;
        this.failReason = failReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * PG 호출 직전에 먼저 저장하는 row.
     * 이후 승인/실패/미확정 상태는 같은 payment row를 update하는 방식으로 전이한다.
     */
    public static PaymentTransaction request(Long orderId,
                                             Long userId,
                                             BigDecimal amount,
                                             String currency,
                                             String idempotencyKey,
                                             String provider,
                                             String reservationId,
                                             String sagaId,
                                             String correlationId,
                                             String sourceEventId) {
        Instant now = Instant.now();
        return new PaymentTransaction(
                null,
                orderId,
                userId,
                amount,
                currency,
                idempotencyKey,
                provider,
                null,
                reservationId,
                sagaId,
                correlationId,
                sourceEventId,
                PaymentStatus.REQUESTED,
                now,
                null,
                null,
                null,
                null,
                null
        );
    }

    public PaymentTransaction markApproved(String providerTxId, Instant approvedAt) {
        return new PaymentTransaction(
                this.paymentId,
                this.orderId,
                this.userId,
                this.amount,
                this.currency,
                this.idempotencyKey,
                this.provider,
                providerTxId,
                this.reservationId,
                this.sagaId,
                this.correlationId,
                this.sourceEventId,
                PaymentStatus.APPROVED,
                this.requestedAt,
                approvedAt,
                null,
                null,
                this.createdAt,
                this.updatedAt
        );
    }

    public PaymentTransaction markFailed(String failReason, Instant failedAt) {
        return new PaymentTransaction(
                this.paymentId,
                this.orderId,
                this.userId,
                this.amount,
                this.currency,
                this.idempotencyKey,
                this.provider,
                this.providerTxId,
                this.reservationId,
                this.sagaId,
                this.correlationId,
                this.sourceEventId,
                PaymentStatus.FAILED,
                this.requestedAt,
                null,
                failedAt,
                failReason,
                this.createdAt,
                this.updatedAt
        );
    }

    public PaymentTransaction markApprovalUnknown(String failReason) {
        return new PaymentTransaction(
                this.paymentId,
                this.orderId,
                this.userId,
                this.amount,
                this.currency,
                this.idempotencyKey,
                this.provider,
                this.providerTxId,
                this.reservationId,
                this.sagaId,
                this.correlationId,
                this.sourceEventId,
                PaymentStatus.APPROVAL_UNKNOWN,
                this.requestedAt,
                null,
                null,
                failReason,
                this.createdAt,
                this.updatedAt
        );
    }

    public static PaymentTransaction rehydrate(Long paymentId,
                                               Long orderId,
                                               Long userId,
                                               BigDecimal amount,
                                               String currency,
                                               String idempotencyKey,
                                               String provider,
                                               String providerTxId,
                                               String reservationId,
                                               String sagaId,
                                               String correlationId,
                                               String sourceEventId,
                                               PaymentStatus status,
                                               Instant requestedAt,
                                               Instant approvedAt,
                                               Instant failedAt,
                                               String failReason,
                                               Instant createdAt,
                                               Instant updatedAt) {
        return new PaymentTransaction(
                paymentId,
                orderId,
                userId,
                amount,
                currency,
                idempotencyKey,
                provider,
                providerTxId,
                reservationId,
                sagaId,
                correlationId,
                sourceEventId,
                status,
                requestedAt,
                approvedAt,
                failedAt,
                failReason,
                createdAt,
                updatedAt
        );
    }

    public Long getPaymentId() { return paymentId; }
    public Long getOrderId() { return orderId; }
    public Long getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getProvider() { return provider; }
    public String getProviderTxId() { return providerTxId; }
    public String getReservationId() { return reservationId; }
    public String getSagaId() { return sagaId; }
    public String getCorrelationId() { return correlationId; }
    public String getSourceEventId() { return sourceEventId; }
    public PaymentStatus getStatus() { return status; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getApprovedAt() { return approvedAt; }
    public Instant getFailedAt() { return failedAt; }
    public String getFailReason() { return failReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public boolean isApproved() { return status == PaymentStatus.APPROVED; }
    public boolean isFailed() { return status == PaymentStatus.FAILED; }
    public boolean isApprovalUnknown() { return status == PaymentStatus.APPROVAL_UNKNOWN; }
}