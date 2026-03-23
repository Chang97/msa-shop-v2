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

    /**
     * 현재 relay를 수행 중인 worker 식별자.
     * 쿠버네티스에서는 HOSTNAME이 pod 이름으로 들어오므로
     * 어느 pod가 row를 선점했는지 추적할 수 있다.
     */
    @Value("${app.kafka.producer.worker-id:unknown-worker}")
    private String workerId;

    /**
     * PROCESSING 상태로 오래 남은 row를 stale lock으로 판단하는 기준 시간(초).
     */
    @Value("${app.kafka.producer.claim-timeout-seconds:300}")
    private long claimTimeoutSeconds;

    @Value("${app.kafka.producer.max-retry-count:5}")
    private int maxRetryCount;

    @Value("${app.kafka.producer.retry-delay-seconds:30}")
    private long retryDelaySeconds;

    @Scheduled(fixedDelayString = "${app.kafka.producer.fixed-delay-ms:3000}")
    public void relay() {
        Instant now = Instant.now();

        // 죽은 pod가 남기고 간 PROCESSING row를 먼저 회수한다.
        outboxEventPort.releaseStaleClaims(now.minusSeconds(claimTimeoutSeconds));

        // 이번 주기에 현재 worker가 처리할 row만 선점한다.
        List<OutboxEventJpaEntity> events = outboxEventPort.claimPending(workerId, batchSize, now);

        for (OutboxEventJpaEntity event : events) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getEventKey(), event.getPayloadJson()).get();
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
