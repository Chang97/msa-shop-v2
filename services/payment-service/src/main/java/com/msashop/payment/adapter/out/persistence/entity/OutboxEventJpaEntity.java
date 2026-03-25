package com.msashop.payment.adapter.out.persistence.entity;

import com.msashop.payment.domain.model.OutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Column(length = 100)
    private String lockedBy;

    private Instant lockedAt;
    private Instant nextRetryAt;

    public void markProcessing(String workerId, Instant now) {
        this.status = OutboxStatus.PROCESSING;
        this.lockedBy = workerId;
        this.lockedAt = now;
        this.lastError = null;
    }

    public void markPublished(Instant now) {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = now;
        this.lockedBy = null;
        this.lockedAt = null;
        this.lastError = null;
        this.nextRetryAt = null;
    }

    public void scheduleRetry(String error, Instant nextRetryAt) {
        this.status = OutboxStatus.PENDING;
        this.retryCount = this.retryCount + 1;
        this.lastError = error;
        this.lockedBy = null;
        this.lockedAt = null;
        this.nextRetryAt = nextRetryAt;
    }

    public void markFailed(String error) {
        this.status = OutboxStatus.FAILED;
        this.retryCount = this.retryCount + 1;
        this.lastError = error;
        this.lockedBy = null;
        this.lockedAt = null;
        this.nextRetryAt = null;
    }
}
