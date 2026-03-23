package com.msashop.auth.adapter.out.persistence.entity;

import com.msashop.auth.domain.model.ProcessedEventStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "processed_event",
        uniqueConstraints = @UniqueConstraint(name = "ux_processed_event", columnNames = {"consumerGroup", "eventId"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEventJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long processedEventId;

    @Column(nullable = false, length = 150)
    private String consumerGroup;

    @Column(nullable = false, length = 36)
    private String eventId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 200)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProcessedEventStatus status;

    /**
     * 최종적으로 처리가 완료된 시각.
     * PROCESSING 단계에서는 null일 수 있다.
     */
    private Instant processedAt;

    @Column(length = 100)
    private String lockedBy;

    private Instant lockedAt;

    private String lastError;
}