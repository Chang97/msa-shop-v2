package com.msashop.payment.adapter.out.persistence.mapper;

import com.msashop.payment.adapter.out.persistence.entity.PaymentTransactionEntity;
import com.msashop.payment.domain.model.PaymentTransaction;

public final class PaymentTransactionMapper {
    private PaymentTransactionMapper() {}

    public static PaymentTransaction toDomain(PaymentTransactionEntity entity) {
        return PaymentTransaction.rehydrate(
                entity.getPaymentId(),
                entity.getOrderId(),
                entity.getUserId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getIdempotencyKey(),
                entity.getProvider(),
                entity.getProviderTxId(),
                entity.getStatus(),
                entity.getRequestedAt(),
                entity.getApprovedAt(),
                entity.getFailedAt(),
                entity.getFailReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static PaymentTransactionEntity toEntity(PaymentTransaction payment) {
        return PaymentTransactionEntity.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .idempotencyKey(payment.getIdempotencyKey())
                .provider(payment.getProvider())
                .providerTxId(payment.getProviderTxId())
                .status(payment.getStatus())
                .requestedAt(payment.getRequestedAt())
                .approvedAt(payment.getApprovedAt())
                .failedAt(payment.getFailedAt())
                .failReason(payment.getFailReason())
                .build();
    }
}

