package com.msashop.product.adapter.out.persistence.repo;

import com.msashop.product.adapter.out.persistence.entity.ProcessedEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventJpaEntity, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO processed_event (
                consumer_group,
                event_id,
                event_type,
                topic,
                status,
                locked_by,
                locked_at,
                processed_at,
                last_error
            )
            VALUES (
                :consumerGroup,
                :eventId,
                :eventType,
                :topic,
                'PROCESSING',
                :workerId,
                :now,
                NULL,
                NULL
            )
            ON CONFLICT (consumer_group, event_id) DO NOTHING
            """, nativeQuery = true)
    int tryInsertProcessing(
            @Param("consumerGroup") String consumerGroup,
            @Param("eventId") String eventId,
            @Param("eventType") String eventType,
            @Param("topic") String topic,
            @Param("workerId") String workerId,
            @Param("now") Instant now
    );

    @Modifying
    @Query(value = """
            UPDATE processed_event
            SET status = 'PROCESSING',
                locked_by = :workerId,
                locked_at = :now,
                last_error = NULL
            WHERE consumer_group = :consumerGroup
              AND event_id = :eventId
              AND (
                    status = 'PENDING'
                    OR (status = 'PROCESSING' AND locked_at < :staleThreshold)
                  )
            """, nativeQuery = true)
    int takeOverClaim(
            @Param("consumerGroup") String consumerGroup,
            @Param("eventId") String eventId,
            @Param("workerId") String workerId,
            @Param("now") Instant now,
            @Param("staleThreshold") Instant staleThreshold
    );

    @Modifying
    @Query(value = """
            UPDATE processed_event
            SET status = 'PROCESSED',
                processed_at = :processedAt,
                locked_by = NULL,
                locked_at = NULL,
                last_error = NULL
            WHERE consumer_group = :consumerGroup
              AND event_id = :eventId
            """, nativeQuery = true)
    int markProcessed(
            @Param("consumerGroup") String consumerGroup,
            @Param("eventId") String eventId,
            @Param("processedAt") Instant processedAt
    );

    @Modifying
    @Query(value = """
            UPDATE processed_event
            SET status = 'PENDING',
                locked_by = NULL,
                locked_at = NULL,
                last_error = :errorMessage
            WHERE consumer_group = :consumerGroup
              AND event_id = :eventId
              AND status = 'PROCESSING'
            """, nativeQuery = true)
    int releaseClaim(
            @Param("consumerGroup") String consumerGroup,
            @Param("eventId") String eventId,
            @Param("errorMessage") String errorMessage
    );
}

