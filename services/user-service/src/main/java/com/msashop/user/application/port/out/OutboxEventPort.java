package com.msashop.user.application.port.out;

import com.msashop.common.event.EventEnvelope;
import com.msashop.user.adapter.out.persistence.entity.OutboxEventJpaEntity;

import java.time.Instant;
import java.util.List;

public interface OutboxEventPort {
    void append(EventEnvelope envelope);

    /**
     * 이번 worker가 처리할 PENDING row를 선점해서 가져온다.
     * 멀티 인스턴스 환경에서 같은 row를 여러 relay가 동시에 집지 않도록
     * DB row lock + PROCESSING 상태 전환을 함께 사용한다.
     */
    List<OutboxEventJpaEntity> claimPending(String workerId, int limit, Instant now);

    /**
     * Kafka 발행이 성공한 row를 최종 완료 상태로 바꾼다.
     */
    void markPublished(Long outboxEventId, Instant publishedAt);

    /**
     * 발행 실패 시 retry_count를 올리고,
     * 최대 재시도 횟수를 넘지 않으면 PENDING + next_retry_at으로 되돌린다.
     * 한계를 넘으면 FAILED로 격리한다.
     */
    void handlePublishFailure(
            Long outboxEventId,
            String errorMessage,
            Instant now,
            int maxRetryCount,
            long retryDelaySeconds
    );

    int releaseStaleClaims(Instant threshold);
}