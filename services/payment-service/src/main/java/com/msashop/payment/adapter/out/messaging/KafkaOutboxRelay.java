package com.msashop.payment.adapter.out.messaging;

import com.msashop.payment.adapter.out.persistence.entity.OutboxEventJpaEntity;
import com.msashop.payment.application.port.out.OutboxEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka.producer", name = "relay-enabled", havingValue = "true")
public class KafkaOutboxRelay {

    private final OutboxEventPort outboxEventPort;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.producer.batch-size:100}")
    private int batchSize;

    @Value("${app.kafka.producer.worker-id:unknown-worker}")
    private String workerId;

    @Value("${app.kafka.producer.claim-timeout-seconds:300}")
    private long claimTimeoutSeconds;

    @Value("${app.kafka.producer.max-retry-count:5}")
    private int maxRetryCount;

    @Value("${app.kafka.producer.retry-delay-seconds:30}")
    private long retryDelaySeconds;

    @Value("${app.kafka.producer.send-timeout-seconds:5}")
    private long sendTimeoutSeconds;

    @Scheduled(fixedDelayString = "${app.kafka.producer.fixed-delay-ms:3000}")
    public void relay() {
        Instant now = Instant.now();
        outboxEventPort.releaseStaleClaims(now.minusSeconds(claimTimeoutSeconds));
        List<OutboxEventJpaEntity> events = outboxEventPort.claimPending(workerId, batchSize, now);

        for (OutboxEventJpaEntity event : events) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getEventKey(), event.getPayloadJson())
                        .get(sendTimeoutSeconds, TimeUnit.SECONDS);
                outboxEventPort.markPublished(event.getOutboxEventId(), Instant.now());
            } catch (Exception e) {
                outboxEventPort.handlePublishFailure(
                        event.getOutboxEventId(),
                        e.getMessage(),
                        Instant.now(),
                        maxRetryCount,
                        retryDelaySeconds
                );
            }
        }
    }
}
