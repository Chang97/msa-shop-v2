package com.msashop.payment.application.port.out;

public interface MarkOrderPaidPort {
    void markPaid(Long orderId, Long paymentId, String idempotencyKey, String reason);
}

