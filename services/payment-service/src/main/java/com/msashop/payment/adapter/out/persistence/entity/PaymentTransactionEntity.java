package com.msashop.payment.adapter.out.persistence.entity;

import com.msashop.payment.domain.model.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_transaction")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransactionEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "idempotency_key", nullable = false, length = 80, unique = true)
    private String idempotencyKey;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_tx_id", length = 100)
    private String providerTxId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "fail_reason")
    private String failReason;
}

