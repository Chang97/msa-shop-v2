package com.msashop.payment.application.port.in.model;

import com.msashop.payment.domain.model.PaymentStatus;

public record PaymentResult(
        Long paymentId,
        Long orderId,
        PaymentStatus status
) {}

