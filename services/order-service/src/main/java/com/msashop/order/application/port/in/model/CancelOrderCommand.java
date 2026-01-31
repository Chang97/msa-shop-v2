package com.msashop.order.application.port.in.model;

public record CancelOrderCommand(
        Long orderId,
        Long userId,
        String reason
) {}

