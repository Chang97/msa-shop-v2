package com.msashop.payment.application.port.out;

import com.msashop.common.event.EventEnvelope;
import com.msashop.payment.adapter.out.persistence.entity.OutboxEventJpaEntity;

import java.time.Instant;
import java.util.List;

public interface OutboxEventPort {
    void append(EventEnvelope envelope);

    List<OutboxEventJpaEntity> claimPending(String workerId, int limit, Instant now);

    void markPublished(Long outboxEventId, Instant publishedAt);

    void handlePublishFailure(
            Long outboxEventId,
            String errorMessage,
            Instant now,
            int maxRetryCount,
            long retryDelaySeconds
    );

    int releaseStaleClaims(Instant threshold);
}
