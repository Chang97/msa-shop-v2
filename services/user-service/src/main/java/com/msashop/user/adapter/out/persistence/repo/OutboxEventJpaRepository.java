package com.msashop.user.adapter.out.persistence.repo;

import com.msashop.user.adapter.out.persistence.entity.OutboxEventJpaEntity;
import com.msashop.user.domain.model.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, Long> {

    /**
     * 현재 시각 기준으로 재시도 가능한 PENDING row만 선점 대상으로 가져온다.
     * next_retry_at이 null이면 즉시 재시도 가능,
     * next_retry_at이 과거/현재면 재시도 가능으로 본다.
     */
    @Query(value = """
              SELECT *
              FROM outbox_event
              WHERE status = 'PENDING'
                AND (next_retry_at IS NULL OR next_retry_at <= :now)
              ORDER BY outbox_event_id
              FOR UPDATE SKIP LOCKED
              LIMIT :limit
              """, nativeQuery = true)
    List<OutboxEventJpaEntity> findClaimable(
            @Param("limit") int limit,
            @Param("now") Instant now
    );

    @Modifying
    @Query(value = """
              UPDATE outbox_event
              SET status = 'PENDING',
                  locked_by = NULL,
                  locked_at = NULL
              WHERE status = 'PROCESSING'
                AND locked_at < :threshold
              """, nativeQuery = true)
    int releaseStaleClaims(@Param("threshold") Instant threshold);
}
