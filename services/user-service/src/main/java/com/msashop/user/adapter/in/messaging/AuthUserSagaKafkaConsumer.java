package com.msashop.user.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.InvalidSagaMessageException;
import com.msashop.user.adapter.out.messaging.SagaDeadLetterPublisher;
import com.msashop.user.application.port.in.HandleAuthUserCreatedSagaUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka.consumers", name = "auth-user-saga-enabled", havingValue = "true")
public class AuthUserSagaKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final HandleAuthUserCreatedSagaUseCase handleAuthUserCreatedSagaUseCase;
    private final SagaDeadLetterPublisher sagaDeadLetterPublisher;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @Value("${app.kafka.consumers.worker-id:${spring.application.name}}")
    private String workerId;

    @Value("${app.kafka.consumers.claim-timeout-seconds:300}")
    private long claimTimeoutSeconds;

    @Value("${app.kafka.topics.auth-user-saga}")
    private String sagaTopic;

    @Value("${app.kafka.topics.auth-user-saga-dlq}")
    private String dlqTopic;

    @KafkaListener(topics = "${app.kafka.topics.auth-user-saga}")
    public void onMessage(String rawMessage, Acknowledgment ack) throws Exception {
        EventEnvelope envelope;
        try {
            envelope = objectMapper.readValue(rawMessage, EventEnvelope.class);
        } catch (Exception e) {
            sagaDeadLetterPublisher.publish(
                    dlqTopic,
                    sagaTopic,
                    consumerGroup,
                    "EVENT_ENVELOPE_DESERIALIZATION_FAILED",
                    e.getMessage(),
                    rawMessage
            );
            ack.acknowledge();
            return;
        }

        // user-service는 saga 시작 이벤트만 처리한다.
        if (!EventTypes.AUTH_USER_CREATED.equals(envelope.eventType())) {
            ack.acknowledge();
            return;
        }

        try {
            boolean handled = handleAuthUserCreatedSagaUseCase.handle(
                    consumerGroup,
                    workerId,
                    claimTimeoutSeconds,
                    envelope
            );

            if (handled) {
                ack.acknowledge();
            }
        } catch (InvalidSagaMessageException e) {
            sagaDeadLetterPublisher.publish(
                    dlqTopic,
                    sagaTopic,
                    consumerGroup,
                    e.reasonCode(),
                    e.getMessage(),
                    rawMessage
            );
            ack.acknowledge();
        }
    }
}
