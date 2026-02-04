package com.msashop.order.application.port.in.model;

public record PayOrderCommand(
        Long orderId,
        Long userId
) {}

