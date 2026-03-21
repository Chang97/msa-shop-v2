package com.msashop.auth.adapter.out.persistence.adapter;

import com.msashop.auth.application.port.out.ProcessedEventPort;
import com.msashop.common.event.EventEnvelope;
import org.springframework.stereotype.Component;

@Component
public class ProcessedEventPersistenceAdapter implements ProcessedEventPort {
    @Override
    public boolean exists(String consumerGroup, String eventId) {
        return false;
    }

    @Override
    public void save(String consumerGroup, EventEnvelope envelope) {

    }
}
