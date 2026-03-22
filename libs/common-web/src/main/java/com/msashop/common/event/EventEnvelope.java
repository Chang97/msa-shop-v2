package com.msashop.common.event;

import java.time.Instant;

/**
 * 서비스 간에 공통으로 사용하는 표준 이벤트 엔벨로프이다.
 *
 * @param eventId 멱등 처리와 추적에 사용하는 전역 고유 이벤트 식별자
 * @param eventType 예: {@code AuthUserRegistered} 같은 의미적 이벤트 이름
 * @param aggregateType 이벤트를 발생시킨 애그리거트 종류
 * @param aggregateId 소스 서비스 내부의 애그리거트 식별자
 * @param sagaId 사가 흐름에 참여하는 경우 사용하는 사가 실행 식별자
 * @param correlationId 관련 명령과 이벤트를 묶어서 추적하기 위한 상관관계 식별자
 * @param causationId 현재 이벤트를 직접 유발한 상위 명령 또는 이벤트 식별자
 * @param sourceService 이벤트를 최초로 발행한 서비스 이름
 * @param topic 이벤트를 발행할 Kafka 토픽 이름
 * @param eventKey 파티셔닝과 키 단위 순서 보장을 위한 Kafka 메시지 키
 * @param occurredAt 비즈니스 이벤트가 실제로 발생한 시각
 * @param payloadJson 비즈니스 payload를 JSON 문자열로 직렬화한 값
 */
public record EventEnvelope(
        String eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        String sagaId,
        String correlationId,
        String causationId,
        String sourceService,
        String topic,
        String eventKey,
        Instant occurredAt,
        String payloadJson
) {
}
