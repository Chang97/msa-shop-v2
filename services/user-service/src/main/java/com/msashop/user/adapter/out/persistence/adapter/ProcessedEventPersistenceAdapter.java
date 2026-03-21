package com.msashop.user.adapter.out.persistence.adapter;

import com.msashop.common.event.EventEnvelope;
import com.msashop.user.application.port.out.ProcessedEventPort;
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
