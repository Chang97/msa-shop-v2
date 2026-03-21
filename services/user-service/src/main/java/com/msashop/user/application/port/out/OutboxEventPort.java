package com.msashop.user.application.port.out;

import com.msashop.common.event.EventEnvelope;
import com.msashop.user.adapter.out.persistence.entity.OutboxEventJpaEntity;

import java.time.Instant;
import java.util.List;

public interface OutboxEventPort {
    void append(EventEnvelope envelope);
    List<OutboxEventJpaEntity> loadPending(int limit);
    void markPublished(Long outboxEventId, Instant publishedAt);
    void markFailed(Long outboxEventId, String errorMessage);
}
