package com.msashop.order.domain.model;

public enum OrderStatus {
    CREATED,
    PENDING_PAYMENT,
    PAYMENT_FAILED,
    PAYMENT_EXPIRED,
    PAID,
    CANCELLED
}
