package com.msashop.payment.unit.domain.model;

import com.msashop.payment.domain.model.PaymentStatus;
import com.msashop.payment.domain.model.PaymentTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PaymentTransactionTest {

    @Test
    @DisplayName("request는 REQUESTED 상태의 결제 객체를 생성한다")
    void should_create_requested_payment() {
        PaymentTransaction payment = PaymentTransaction.request(
                1L,
                1L,
                new BigDecimal("10000"),
                "KRW",
                "idem-1",
                "FAKE",
                "reservation-1",
                "saga-1",
                "corr-1",
                "event-1"
        );

        assertEquals(PaymentStatus.REQUESTED, payment.getStatus());
        assertEquals("idem-1", payment.getIdempotencyKey());
        assertNotNull(payment.getRequestedAt());
        assertNull(payment.getApprovedAt());
        assertNull(payment.getFailedAt());
    }

    @Test
    @DisplayName("markApproved는 APPROVED 상태와 providerTxId를 반영한다")
    void should_mark_payment_approved() {
        PaymentTransaction requested = requestedPayment();
        Instant approvedAt = Instant.now();

        PaymentTransaction approved = requested.markApproved("pg-tx-1", approvedAt);

        assertEquals(PaymentStatus.APPROVED, approved.getStatus());
        assertEquals("pg-tx-1", approved.getProviderTxId());
        assertEquals(approvedAt, approved.getApprovedAt());
    }

    @Test
    @DisplayName("markFailed는 FAILED 상태와 실패 사유를 반영한다")
    void should_mark_payment_failed() {
        PaymentTransaction requested = requestedPayment();
        Instant failedAt = Instant.now();

        PaymentTransaction failed = requested.markFailed("forced failure", failedAt);

        assertEquals(PaymentStatus.FAILED, failed.getStatus());
        assertEquals("forced failure", failed.getFailReason());
        assertEquals(failedAt, failed.getFailedAt());
    }

    @Test
    @DisplayName("markApprovalUnknown은 APPROVAL_UNKNOWN 상태를 반영한다")
    void should_mark_payment_approval_unknown() {
        PaymentTransaction requested = requestedPayment();

        PaymentTransaction unknown = requested.markApprovalUnknown("timeout");

        assertEquals(PaymentStatus.APPROVAL_UNKNOWN, unknown.getStatus());
        assertEquals("timeout", unknown.getFailReason());
    }

    private PaymentTransaction requestedPayment() {
        return PaymentTransaction.request(
                1L,
                1L,
                new BigDecimal("10000"),
                "KRW",
                "idem-1",
                "FAKE",
                "reservation-1",
                "saga-1",
                "corr-1",
                "event-1"
        );
    }
}
