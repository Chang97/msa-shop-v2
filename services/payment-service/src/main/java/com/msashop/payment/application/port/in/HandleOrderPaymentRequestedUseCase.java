package com.msashop.payment.application.port.in;

import com.msashop.common.event.EventEnvelope;

public interface HandleOrderPaymentRequestedUseCase {
    boolean handle(String consumerGroup, String workerId, long claimTimeoutSeconds, EventEnvelope envelope) throws Exception;
}