package com.msashop.auth.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.auth.adapter.out.messaging.SagaDeadLetterPublisher;
import com.msashop.auth.application.port.in.HandleAuthUserSagaCompletionUseCase;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.SagaConsumerSupport;
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
        SagaConsumerSupport.consume(
                rawMessage,
                ack::acknowledge,
                this::parseEnvelope,
                consumerSpec(),
                envelope -> handleAuthUserSagaCompletionUseCase.handle(
                        consumerGroup,
                        workerId,
                        claimTimeoutSeconds,
                        envelope
                ),
                sagaDeadLetterPublisher
        );
    }

    private EventEnvelope parseEnvelope(String rawMessage) throws Exception {
        return objectMapper.readValue(rawMessage, EventEnvelope.class);
    }

    private SagaConsumerSupport.ConsumerSpec consumerSpec() {
        return new SagaConsumerSupport.ConsumerSpec(
                sagaTopic,
                dlqTopic,
                consumerGroup,
                eventType -> EventTypes.USER_PROFILE_CREATED.equals(eventType)
                        || EventTypes.USER_PROFILE_CREATION_FAILED.equals(eventType)
                        || EventTypes.USER_DEACTIVATED.equals(eventType)
        );
    }
}
