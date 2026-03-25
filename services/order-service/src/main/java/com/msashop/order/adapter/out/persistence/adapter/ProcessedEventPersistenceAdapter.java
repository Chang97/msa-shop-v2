package com.msashop.order.adapter.out.persistence.adapter;

import com.msashop.common.event.EventEnvelope;
import com.msashop.order.adapter.out.persistence.repo.ProcessedEventJpaRepository;
import com.msashop.order.application.port.out.ProcessedEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ProcessedEventPersistenceAdapter implements ProcessedEventPort {

    private final ProcessedEventJpaRepository repository;

    @Override
    @Transactional
    public boolean claim(String consumerGroup, EventEnvelope envelope, String workerId, Instant now, Instant staleThreshold) {
        int inserted = repository.tryInsertProcessing(
                consumerGroup,
                envelope.eventId(),
                envelope.eventType(),
                envelope.topic(),
                workerId,
                now
        );
        if (inserted == 1) {
            return true;
        }
        return repository.takeOverClaim(
                consumerGroup,
                envelope.eventId(),
                workerId,
                now,
                staleThreshold
        ) == 1;
    }

    @Override
    @Transactional
    public void markProcessed(String consumerGroup, String eventId, Instant processedAt) {
        repository.markProcessed(consumerGroup, eventId, processedAt);
    }

    @Override
    @Transactional
    public void releaseClaim(String consumerGroup, String eventId, String errorMessage) {
        repository.releaseClaim(consumerGroup, eventId, errorMessage);
    }
}
