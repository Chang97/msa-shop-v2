package com.msashop.payment.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.DeadLetterEnvelope;
import com.msashop.common.event.DeadLetterPublisherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class SagaDeadLetterPublisher implements DeadLetterPublisherPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(
            String dlqTopic,
            String originalTopic,
            String consumerGroup,
            String reasonCode,
            String reasonMessage,
            String originalMessage
    ) {
        try {
            DeadLetterEnvelope deadLetterEnvelope = new DeadLetterEnvelope(
                    originalTopic,
                    "payment-service",
                    consumerGroup,
                    reasonCode,
                    reasonMessage,
                    Instant.now(),
                    originalMessage
            );

            kafkaTemplate.send(dlqTopic, reasonCode, objectMapper.writeValueAsString(deadLetterEnvelope)).get();
        } catch (Exception e) {
            throw new IllegalStateException("Dead letter publish failed.", e);
        }
    }
}
