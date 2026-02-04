package com.msashop.payment.application.port.in.model;

import com.msashop.payment.common.response.CurrentUser;

import java.math.BigDecimal;

public record ApprovePaymentCommand(
        Long orderId,
        BigDecimal amount,
        String idempotencyKey,
        CurrentUser currentUser
) {}

