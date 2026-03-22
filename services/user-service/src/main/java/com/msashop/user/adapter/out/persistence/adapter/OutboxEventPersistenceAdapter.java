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
        // 현재 relay는 payloadJson 컬럼 값을 Kafka로 그대로 보내므로
        // 여기서는 EventEnvelope 전체를 JSON 문자열로 저장한다.
        // 컬럼명이 payload_json이라 약간 어색하지만, 지금 스켈레톤을 최소 수정으로 살리는 방법이다.
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
    @Transactional(readOnly = true)
    public List<OutboxEventJpaEntity> loadPending(int limit) {
        return repository.findTop100ByStatusOrderByOutboxEventIdAsc(OutboxStatus.PENDING);
    }

    @Override
    @Transactional
    public void markPublished(Long outboxEventId, Instant publishedAt) {
        OutboxEventJpaEntity entity = repository.findById(outboxEventId).orElseThrow();
        entity.markPublished(publishedAt);
    }

    @Override
    @Transactional
    public void markFailed(Long outboxEventId, String errorMessage) {
        OutboxEventJpaEntity entity = repository.findById(outboxEventId).orElseThrow();
        entity.markFailed(errorMessage);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Outbox 메시지 직렬화에 실패했습니다.", e);
        }
    }
}
