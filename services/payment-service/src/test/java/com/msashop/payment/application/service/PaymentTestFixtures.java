package com.msashop.payment.application.service;

import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.payload.StockReservedPayload;
import com.msashop.payment.domain.model.PaymentStatus;
import com.msashop.payment.domain.model.PaymentTransaction;

import java.math.BigDecimal;
import java.time.Instant;

public final class PaymentTestFixtures {

    private PaymentTestFixtures() {
    }

    public static EventEnvelope stockReservedEvent() {
        return new EventEnvelope(
                "event-1",
                EventTypes.STOCK_RESERVED,
                "STOCK_RESERVATION",
                "reservation-1",
                "saga-1",
                "corr-1",
                "cause-1",
                "product-service",
                "order.payment.saga.v1",
                "1",
                Instant.now(),
                "{\"orderId\":1}"
        );
    }

    public static StockReservedPayload stockReservedPayload() {
        return new StockReservedPayload(
                1L,
                1L,
                "reservation-1",
                new BigDecimal("10000"),
                "KRW",
                "idem-1",
                "FAKE"
        );
    }

    public static PaymentTransaction payment(PaymentStatus status) {
        return PaymentTransaction.rehydrate(
                100L,
                1L,
                1L,
                new BigDecimal("10000"),
                "KRW",
                "idem-1",
                "FAKE",
                null,
                "reservation-1",
                "saga-1",
                "corr-1",
                "event-1",
                status,
                Instant.now(),
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );
    }
}
