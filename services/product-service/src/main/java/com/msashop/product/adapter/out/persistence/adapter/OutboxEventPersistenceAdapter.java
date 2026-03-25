package com.msashop.product.adapter.out.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.product.adapter.out.persistence.entity.OutboxEventJpaEntity;
import com.msashop.product.adapter.out.persistence.repo.OutboxEventJpaRepository;
import com.msashop.product.application.port.out.OutboxEventPort;
import com.msashop.product.domain.model.OutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxEventPersistenceAdapter implements OutboxEventPort {

    private final OutboxEventJpaRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void append(EventEnvelope envelope) {
        repository.save(OutboxEventJpaEntity.builder()
                .eventId(envelope.eventId())
                .topic(envelope.topic())
                .eventKey(envelope.eventKey())
                .eventType(envelope.eventType())
                .aggregateType(envelope.aggregateType())
                .aggregateId(envelope.aggregateId())
                .sagaId(envelope.sagaId())
                .correlationId(envelope.correlationId())
                .causationId(envelope.causationId())
                .payloadJson(writeJson(envelope))
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build());
    }

    @Override
    @Transactional
    public List<OutboxEventJpaEntity> claimPending(String workerId, int limit, Instant now) {
        List<OutboxEventJpaEntity> events = repository.findClaimable(limit, now);
        for (OutboxEventJpaEntity event : events) {
            event.markProcessing(workerId, now);
        }
        return events;
    }

    @Override
    @Transactional
    public void markPublished(Long outboxEventId, Instant publishedAt) {
        repository.findById(outboxEventId).orElseThrow().markPublished(publishedAt);
    }

    @Override
    @Transactional
    public void handlePublishFailure(Long outboxEventId, String errorMessage, Instant now, int maxRetryCount, long retryDelaySeconds) {
        OutboxEventJpaEntity entity = repository.findById(outboxEventId).orElseThrow();
        int nextRetryCount = entity.getRetryCount() + 1;
        if (nextRetryCount >= maxRetryCount) {
            entity.markFailed(errorMessage);
            return;
        }
        entity.scheduleRetry(errorMessage, now.plusSeconds(retryDelaySeconds));
    }

    @Override
    @Transactional
    public int releaseStaleClaims(Instant threshold) {
        return repository.releaseStaleClaims(threshold);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Outbox message serialization failed.", e);
        }
    }
}

