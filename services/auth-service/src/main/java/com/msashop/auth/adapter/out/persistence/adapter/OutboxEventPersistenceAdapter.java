package com.msashop.auth.adapter.out.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.auth.adapter.out.persistence.entity.OutboxEventJpaEntity;
import com.msashop.auth.adapter.out.persistence.repo.OutboxEventJpaRepository;
import com.msashop.auth.application.port.out.OutboxEventPort;
import com.msashop.auth.domain.model.OutboxStatus;
import com.msashop.common.event.EventEnvelope;
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
        // 현재 relay는 payloadJson 컬럼을 Kafka로 그대로 보내므로
        // outbox row에는 EventEnvelope 전체 JSON 문자열을 저장한다.
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
        // 처음 단계에서는 limit 파라미터를 그대로 쓰지 못해도 동작은 가능하다.
        // 다만 메서드 이름과 실제 limit 값이 어긋나지 않도록 나중에 맞추는 편이 낫다.
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
            throw new IllegalStateException("Outbox 메시지 직렬화에 실패했습니다.", e);
        }
    }
}