package com.msashop.payment.adapter.in.web.dto;

import com.msashop.payment.domain.model.PaymentStatus;

public record PaymentResponse(
        Long paymentId,
        Long orderId,
        PaymentStatus status
) {}

