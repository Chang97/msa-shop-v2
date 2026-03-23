package com.msashop.auth.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.auth.adapter.out.messaging.SagaDeadLetterPublisher;
import com.msashop.auth.application.port.in.HandleAuthUserSagaCompletionUseCase;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.InvalidSagaMessageException;
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
    private final HandleAuthUserSagaCompletionUseCase handleAuthUserSagaCompletionUseCase;
    private final SagaDeadLetterPublisher sagaDeadLetterPublisher;

    @Value("${app.kafka.consumers.worker-id:${spring.application.name}}")
    private String workerId;

    @Value("${app.kafka.consumers.claim-timeout-seconds:300}")
    private long claimTimeoutSeconds;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

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

        // auth-service가 처리할 완료 이벤트만 통과시킨다.
        if (!isAuthCompletionEvent(envelope.eventType())) {
            ack.acknowledge();
            return;
        }

        try {
            boolean handled = handleAuthUserSagaCompletionUseCase.handle(
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

    /**
     * auth-service가 사가 종료를 위해 받아야 하는 이벤트만 정의한다.
     * 시작 이벤트(AuthUserCreated)는 auth가 보낸 이벤트이므로 다시 처리하지 않는다.
     */
    private boolean isAuthCompletionEvent(String eventType) {
        return EventTypes.USER_PROFILE_CREATED.equals(eventType)
                || EventTypes.USER_PROFILE_CREATION_FAILED.equals(eventType);
    }
}
