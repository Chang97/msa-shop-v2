package com.msashop.user.adapter.out.persistence.entity;

import com.msashop.user.domain.model.OutboxStatus;
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

    public void markPublished(Instant now) {
        // Kafka 발행 성공 시 상태와 시각을 함께 바꾼다.
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = now;
        this.lastError = null;
    }
    public void markFailed(String error) {
        // 발행 실패 시 재시도 횟수와 마지막 에러를 남긴다.
        this.status = OutboxStatus.FAILED;
        this.retryCount = this.retryCount + 1;
        this.lastError = error;
    }
}
