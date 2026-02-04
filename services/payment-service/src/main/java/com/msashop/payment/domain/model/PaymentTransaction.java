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
        this.status = Objects.requireNonNull(status, "status");
        this.requestedAt = requestedAt;
        this.approvedAt = approvedAt;
        this.failedAt = failedAt;
        this.failReason = failReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PaymentTransaction approve(Long orderId,
                                             Long userId,
                                             BigDecimal amount,
                                             String currency,
                                             String idempotencyKey,
                                             String provider,
                                             String providerTxId,
                                             Instant approvedAt) {
        Instant now = Instant.now();
        return new PaymentTransaction(null, orderId, userId, amount, currency, idempotencyKey, provider, providerTxId,
                PaymentStatus.APPROVED, now, approvedAt, null, null, null, null);
    }

    public static PaymentTransaction rehydrate(Long paymentId,
                                               Long orderId,
                                               Long userId,
                                               BigDecimal amount,
                                               String currency,
                                               String idempotencyKey,
                                               String provider,
                                               String providerTxId,
                                               PaymentStatus status,
                                               Instant requestedAt,
                                               Instant approvedAt,
                                               Instant failedAt,
                                               String failReason,
                                               Instant createdAt,
                                               Instant updatedAt) {
        return new PaymentTransaction(paymentId, orderId, userId, amount, currency, idempotencyKey, provider, providerTxId,
                status, requestedAt, approvedAt, failedAt, failReason, createdAt, updatedAt);
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderTxId() {
        return providerTxId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public String getFailReason() {
        return failReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isApproved() {
        return status == PaymentStatus.APPROVED;
    }
}

