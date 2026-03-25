package com.msashop.order.adapter.out.persistence.repo;

import com.msashop.order.adapter.out.persistence.entity.OutboxEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, Long> {

    @Query(value = """
            SELECT *
            FROM outbox_event
            WHERE status = 'PENDING'
              AND (next_retry_at IS NULL OR next_retry_at <= :now)
            ORDER BY outbox_event_id
            FOR UPDATE SKIP LOCKED
            LIMIT :limit
            """, nativeQuery = true)
    List<OutboxEventJpaEntity> findClaimable(@Param("limit") int limit, @Param("now") Instant now);

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
