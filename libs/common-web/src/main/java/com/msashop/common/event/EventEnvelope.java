package com.msashop.common.event;

import java.time.Instant;

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
