package com.msashop.payment.application.port.out;

import com.msashop.payment.domain.model.PaymentTransaction;

import java.util.Optional;

public interface LoadPaymentPort {
    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);
}

