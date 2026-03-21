package com.msashop.user.adapter.out.messaging;

import com.msashop.user.adapter.out.persistence.entity.OutboxEventJpaEntity;
import com.msashop.user.application.port.out.OutboxEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka.producer", name = "relay-enabled", havingValue = "true")
public class KafkaOutboxRelay {

    private final OutboxEventPort outboxEventPort;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.producer.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.kafka.producer.fixed-delay-ms:3000}")
    public void relay() {
        List<OutboxEventJpaEntity> events = outboxEventPort.loadPending(batchSize);
        for (OutboxEventJpaEntity event : events) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getEventKey(), event.getPayloadJson()).get();
                outboxEventPort.markPublished(event.getOutboxEventId(), Instant.now());
            } catch (Exception e) {
                outboxEventPort.markFailed(event.getOutboxEventId(), e.getMessage());
            }
        }
    }
}
