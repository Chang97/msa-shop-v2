package com.msashop.common.event;

import java.time.Instant;

/**
 * 정상 처리할 수 없는 poison message를 DLQ 토픽으로 보낼 때 사용하는 공통 모델.
 *
 * @param originalTopic 원본 메시지를 읽어온 토픽
 * @param sourceService DLQ로 보낸 서비스 이름
 * @param consumerGroup 메시지를 처리하던 consumer group
 * @param reasonCode DLQ로 보낸 기술적 사유 코드
 * @param errorMessage 마지막 오류 메시지
 * @param failedAt DLQ 적재 시각
 * @param originalMessage 원본 Kafka 메시지(raw JSON)
 */
public record DeadLetterEnvelope(
        String originalTopic,
        String sourceService,
        String consumerGroup,
        String reasonCode,
        String errorMessage,
        Instant failedAt,
        String originalMessage
) {
}
