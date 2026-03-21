package com.msashop.user.application.port.out;

import com.msashop.common.event.EventEnvelope;

public interface ProcessedEventPort {
    boolean exists(String consumerGroup, String eventId);
    void save (String consumerGroup, EventEnvelope envelope);
}
