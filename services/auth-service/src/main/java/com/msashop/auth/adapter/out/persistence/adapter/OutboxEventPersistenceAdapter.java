package com.msashop.auth.adapter.out.persistence.adapter;

import com.msashop.auth.adapter.out.persistence.entity.OutboxEventJpaEntity;
import com.msashop.auth.application.port.out.OutboxEventPort;
import com.msashop.common.event.EventEnvelope;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class OutboxEventPersistenceAdapter implements OutboxEventPort {
    @Override
    public void append(EventEnvelope envelope) {

    }

    @Override
    public List<OutboxEventJpaEntity> loadPending(int limit) {
        return List.of();
    }

    @Override
    public void markPublished(Long outboxEventId, Instant publishedAt) {

    }

    @Override
    public void markFailed(Long outboxEventId, String errorMessage) {

    }
}
