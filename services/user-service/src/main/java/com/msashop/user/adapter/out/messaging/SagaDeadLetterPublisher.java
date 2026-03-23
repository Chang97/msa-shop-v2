package com.msashop.user.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.DeadLetterEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * user-service가 처리할 수 없는 poison message를 DLQ 토픽으로 보내는 publisher.
 */
@Component
@RequiredArgsConstructor
public class SagaDeadLetterPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publish(
            String dlqTopic,
            String originalTopic,
            String consumerGroup,
            String reasonCode,
            String errorMessage,
            String originalMessage
    ) {
        try {
            DeadLetterEnvelope deadLetterEnvelope = new DeadLetterEnvelope(
                    originalTopic,
                    "user-service",
                    consumerGroup,
                    reasonCode,
                    errorMessage,
                    Instant.now(),
                    originalMessage
            );

            kafkaTemplate.send(
                    dlqTopic,
                    reasonCode,
                    objectMapper.writeValueAsString(deadLetterEnvelope)
            ).get();
        } catch (Exception e) {
            throw new IllegalStateException("DLQ 메시지 발행에 실패했습니다.", e);
        }
    }
}
