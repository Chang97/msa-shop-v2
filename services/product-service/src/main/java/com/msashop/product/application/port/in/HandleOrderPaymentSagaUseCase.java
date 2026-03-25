package com.msashop.product.application.port.in;

import com.msashop.common.event.EventEnvelope;

public interface HandleOrderPaymentSagaUseCase {
    boolean handle(String consumerGroup, String workerId, long claimTimeoutSeconds, EventEnvelope envelope) throws Exception;
}