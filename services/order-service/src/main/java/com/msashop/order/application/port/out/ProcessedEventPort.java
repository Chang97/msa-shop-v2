package com.msashop.order.application.port.out;

import com.msashop.common.event.EventEnvelope;

import java.time.Instant;

public interface ProcessedEventPort {
    boolean claim(String consumerGroup, EventEnvelope envelope, String workerId, Instant now, Instant staleThreshold);

    void markProcessed(String consumerGroup, String eventId, Instant processedAt);

    void releaseClaim(String consumerGroup, String eventId, String errorMessage);
}
