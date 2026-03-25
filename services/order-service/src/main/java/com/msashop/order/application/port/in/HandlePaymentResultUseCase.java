package com.msashop.order.application.port.in;

import com.msashop.common.event.EventEnvelope;

public interface HandlePaymentResultUseCase {
    boolean handle(String consumerGroup, String workerId, long claimTimeoutSeconds, EventEnvelope envelope) throws Exception;
}