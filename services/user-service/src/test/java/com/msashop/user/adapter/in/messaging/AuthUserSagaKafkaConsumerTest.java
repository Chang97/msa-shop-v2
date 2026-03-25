package com.msashop.user.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.InvalidSagaMessageException;
import com.msashop.user.adapter.out.messaging.SagaDeadLetterPublisher;
import com.msashop.user.application.port.in.HandleAuthUserCreatedSagaUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUserSagaKafkaConsumerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HandleAuthUserCreatedSagaUseCase handleAuthUserCreatedSagaUseCase;

    @Mock
    private SagaDeadLetterPublisher sagaDeadLetterPublisher;

    @Mock
    private Acknowledgment acknowledgment;

    private AuthUserSagaKafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AuthUserSagaKafkaConsumer(
                objectMapper,
                handleAuthUserCreatedSagaUseCase,
                sagaDeadLetterPublisher
        );

        ReflectionTestUtils.setField(consumer, "consumerGroup", "user-service-auth-user-saga");
        ReflectionTestUtils.setField(consumer, "workerId", "user-worker");
        ReflectionTestUtils.setField(consumer, "claimTimeoutSeconds", 300L);
        ReflectionTestUtils.setField(consumer, "sagaTopic", "auth.user.saga.v1");
        ReflectionTestUtils.setField(consumer, "dlqTopic", "auth.user.saga.v1.dlq");
    }

    @Test
    void should_publish_to_dlq_and_ack_when_use_case_throws_invalid_saga_message() throws Exception {
        EventEnvelope sourceEvent = new EventEnvelope(
                "event-1",
                EventTypes.AUTH_USER_CREATED,
                "AUTH_USER",
                "1",
                "saga-1",
                "corr-1",
                null,
                "auth-service",
                "auth.user.saga.v1",
                "1",
                Instant.now(),
                "{\"authUserId\":1}"
        );

        when(objectMapper.readValue("raw-message", EventEnvelope.class)).thenReturn(sourceEvent);
        doThrow(new InvalidSagaMessageException(
                "AUTH_USER_CREATED_PAYLOAD_DESERIALIZATION_FAILED",
                "payload parse failed",
                new RuntimeException("bad payload")
        )).when(handleAuthUserCreatedSagaUseCase).handle(
                "user-service-auth-user-saga",
                "user-worker",
                300L,
                sourceEvent
        );

        consumer.onMessage("raw-message", acknowledgment);

        verify(sagaDeadLetterPublisher).publish(
                "auth.user.saga.v1.dlq",
                "auth.user.saga.v1",
                "user-service-auth-user-saga",
                "AUTH_USER_CREATED_PAYLOAD_DESERIALIZATION_FAILED",
                "payload parse failed",
                "raw-message"
        );
        verify(acknowledgment).acknowledge();
    }
}
