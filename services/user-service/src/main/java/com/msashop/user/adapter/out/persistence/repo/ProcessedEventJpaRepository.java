package com.msashop.user.adapter.out.persistence.repo;

import com.msashop.user.adapter.out.persistence.entity.ProcessedEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventJpaEntity, Long> {
    /**
     * 신규 이벤트라면 PROCESSING row를 삽입하며 처리권을 확보한다.
     * 이미 같은 consumer_group + event_id가 있으면 insert되지 않는다.
     */
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

    /**
     * 기존 row가 PENDING이거나 stale PROCESSING이면 현재 worker가 처리권을 가져온다.
     * 이미 PROCESSED이거나 다른 worker가 정상 처리 중이면 0건 갱신된다.
     */
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

    /**
     * 비즈니스 처리와 후속 outbox 적재까지 끝난 이벤트를 최종 완료로 바꾼다.
     */
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

    /**
     * 인프라 예외로 처리를 끝내지 못한 경우 다시 재시도 가능한 상태로 푼다.
     */
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
