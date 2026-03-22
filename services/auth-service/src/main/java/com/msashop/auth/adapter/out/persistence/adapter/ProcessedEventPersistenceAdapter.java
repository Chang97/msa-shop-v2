package com.msashop.auth.adapter.out.persistence.adapter;

import com.msashop.auth.adapter.out.persistence.entity.ProcessedEventJpaEntity;
import com.msashop.auth.adapter.out.persistence.repo.ProcessedEventJpaRepository;
import com.msashop.auth.application.port.out.ProcessedEventPort;
import com.msashop.common.event.EventEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ProcessedEventPersistenceAdapter implements ProcessedEventPort {

    private final ProcessedEventJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public boolean exists(String consumerGroup, String eventId) {
        return repository.existsByConsumerGroupAndEventId(consumerGroup, eventId);
    }

    @Override
    @Transactional
    public void save(String consumerGroup, EventEnvelope envelope) {
        try {
            repository.save(ProcessedEventJpaEntity.builder()
                    // 같은 consumer group이 같은 eventId를 다시 저장하려고 하면
                    // unique 제약으로 막히도록 한다.
                    .consumerGroup(consumerGroup)
                    .eventId(envelope.eventId())
                    .eventType(envelope.eventType())
                    .topic(envelope.topic())
                    .processedAt(Instant.now())
                    .build());
        } catch (DataIntegrityViolationException e) {
            // 중복 저장 충돌은 이미 처리된 이벤트로 보고 조용히 무시한다.
            // 이렇게 하면 rebalance/재전달 상황에서도 consumer가 덜 예민해진다.
        }
    }
}
