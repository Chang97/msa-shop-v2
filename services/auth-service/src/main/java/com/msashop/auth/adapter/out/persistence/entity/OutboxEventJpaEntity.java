package com.msashop.auth.adapter.out.persistence.entity;

import com.msashop.auth.domain.model.OutboxStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;

@Entity
@Table(name = "outbox_event")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEventJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long outboxEventId;

    @Column(nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(nullable = false, length = 200)
    private String topic;

    @Column(nullable = false, length = 200)
    private String eventKey;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 100)
    private String aggregateType;

    @Column(nullable = false, length = 100)
    private String aggregateId;

    private String sagaId;
    private String correlationId;
    private String causationId;

    /**
     * 현재 구조에서는 Kafka로 보낼 최종 메시지(EventEnvelope 전체 JSON)를 그대로 저장한다.
     * DB 컬럼은 jsonb이므로 write 시 jsonb 캐스팅을 명시한다.
     */
    @Column(nullable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private Integer retryCount;

    private Instant publishedAt;
    private String lastError;

    /**
     * 현재 row를 어느 worker가 가져가 처리 중인지 기록한다.
     * 쿠버네티스에서는 보통 pod 이름(HOSTNAME)을 넣으면 추적이 쉽다.
     */
    @Column(length = 100)
    private String lockedBy;

    /**
     * worker가 row를 선점한 시각.
     * stale lock 회수 기준으로 사용한다.
     */
    private Instant lockedAt;

    /**
     * 다음 재시도 가능 시각.
     * null이면 즉시 재시도 가능 상태로 본다.
     */
    private Instant nextRetryAt;

    public void markProcessing(String workerId, Instant now) {
        this.status = OutboxStatus.PROCESSING;
        this.lockedBy = workerId;
        this.lockedAt = now;
        this.lastError = null;
    }

    public void markPublished(Instant now) {
        // Kafka 발행 성공 시 상태와 발행 시각을 함께 갱신한다.
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = now;
        this.lockedBy = null;
        this.lockedAt = null;
        this.lastError = null;
        this.nextRetryAt = null;
    }

    /**
     * 발행 실패 시 재시도 가능 횟수를 넘지 않으면 PENDING으로 되돌리고,
     * 다음 재시도 가능 시각을 설정한다.
     */
    public void scheduleRetry(String error, Instant nextRetryAt) {
        this.status = OutboxStatus.PENDING;
        this.retryCount = this.retryCount + 1;
        this.lastError = error;
        this.lockedBy = null;
        this.lockedAt = null;
        this.nextRetryAt = nextRetryAt;
    }

    /**
     * 최대 재시도 횟수를 넘긴 경우 FAILED로 최종 격리한다.
     */
    public void markFailed(String error) {
        this.status = OutboxStatus.FAILED;
        this.retryCount = this.retryCount + 1;
        this.lastError = error;
        this.lockedBy = null;
        this.lockedAt = null;
    }
}
