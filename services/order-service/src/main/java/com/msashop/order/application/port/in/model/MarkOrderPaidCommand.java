package com.msashop.order.application.port.in.model;

public record MarkOrderPaidCommand(
        Long orderId,
        Long paymentId,
        String idempotencyKey,
        String reason
) {}

