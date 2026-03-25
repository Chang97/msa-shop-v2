package com.msashop.user.application.port.out;

import com.msashop.common.event.EventEnvelope;

import java.time.Instant;

public interface ProcessedEventPort {
    /**
     * 현재 worker가 이 이벤트 처리권을 확보할 수 있으면 true를 반환한다.
     * - 신규 이벤트면 insert로 선점
     * - 기존 row가 PENDING이면 다시 선점
     * - 기존 row가 오래된 PROCESSING이면 takeover
     * - 이미 PROCESSED이거나 다른 worker가 정상 처리 중이면 false
     */
    boolean claim(String consumerGroup, EventEnvelope envelope, String workerId, Instant now, Instant staleThreshold);

    /**
     * 비즈니스 처리와 후속 outbox 적재가 끝난 뒤 최종 완료 상태로 바꾼다.
     */
    void markProcessed(String consumerGroup, String eventId, Instant processedAt);

    /**
     * 인프라 예외 등으로 처리를 끝내지 못한 경우 처리권을 다시 풀어준다.
     * 이후 재전달 시 다른 worker가 다시 claim할 수 있어야 한다.
     */
    void releaseClaim(String consumerGroup, String eventId, String errorMessage);
}
