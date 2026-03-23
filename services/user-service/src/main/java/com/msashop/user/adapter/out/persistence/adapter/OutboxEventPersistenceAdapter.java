package com.msashop.user.adapter.out.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.user.adapter.out.persistence.entity.OutboxEventJpaEntity;
import com.msashop.user.adapter.out.persistence.repo.OutboxEventJpaRepository;
import com.msashop.user.application.port.out.OutboxEventPort;
import com.msashop.user.domain.model.OutboxStatus;
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
        String messageJson = writeJson(envelope);

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
                .payloadJson(messageJson)
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
        OutboxEventJpaEntity entity = repository.findById(outboxEventId).orElseThrow();
        entity.markPublished(publishedAt);
    }

    @Override
    @Transactional
    public void handlePublishFailure(
            Long outboxEventId,
            String errorMessage,
            Instant now,
            int maxRetryCount,
            long retryDelaySeconds
    ) {
        OutboxEventJpaEntity entity = repository.findById(outboxEventId).orElseThrow();

        // 이번 실패를 반영하면 최대 재시도 횟수를 넘는지 먼저 계산한다.
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
            throw new IllegalStateException("Outbox 메시지 직렬화에 실패했습니다.", e);
        }
    }
}
