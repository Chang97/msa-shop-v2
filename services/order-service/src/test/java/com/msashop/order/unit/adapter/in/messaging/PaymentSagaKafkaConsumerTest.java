package com.msashop.order.unit.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.InvalidSagaMessageException;
import com.msashop.order.adapter.in.messaging.PaymentSagaKafkaConsumer;
import com.msashop.order.adapter.out.messaging.SagaDeadLetterPublisher;
import com.msashop.order.application.port.in.HandlePaymentResultUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * order-service Kafka consumer 테스트.
 *
 * 검증 목적:
 * - listener가 InvalidSagaMessageException을 받았을 때
 *   DLQ publish + ack 흐름만 담당하는지 확인한다
 *
 * 실제 주문 상태 반영 로직은 service 테스트에서 따로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class PaymentSagaKafkaConsumerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HandlePaymentResultUseCase handlePaymentResultUseCase;

    @Mock
    private SagaDeadLetterPublisher sagaDeadLetterPublisher;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private PaymentSagaKafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(consumer, "consumerGroup", "order-service-order-payment-saga");
        ReflectionTestUtils.setField(consumer, "workerId", "order-worker");
        ReflectionTestUtils.setField(consumer, "claimTimeoutSeconds", 300L);
        ReflectionTestUtils.setField(consumer, "sagaTopic", "order.payment.saga.v1");
        ReflectionTestUtils.setField(consumer, "dlqTopic", "order.payment.saga.v1.dlq");
    }

    /**
     * use case가 InvalidSagaMessageException을 던지는 케이스.
     *
     * 기대값:
     * - raw message가 DLQ로 발행된다
     * - Kafka 메시지는 ack 처리된다
     *
     * 즉, 잘못된 메시지가 본 토픽을 막지 않도록 해야 한다.
     */
    @Test
    void should_publish_to_dlq_and_ack_when_use_case_throws_invalid_saga_message() throws Exception {
        EventEnvelope sourceEvent = new EventEnvelope(
                "event-1",
                EventTypes.PAYMENT_APPROVED,
                "PAYMENT",
                "100",
                "saga-1",
                "corr-1",
                "cause-1",
                "payment-service",
                "order.payment.saga.v1",
                "1",
                Instant.now(),
                "{\"orderId\":1}"
        );

        when(objectMapper.readValue("raw-message", EventEnvelope.class)).thenReturn(sourceEvent);
        doThrow(new InvalidSagaMessageException(
                "PAYMENT_APPROVED_PAYLOAD_DESERIALIZATION_FAILED",
                "payload parse failed",
                new RuntimeException("bad payload")
        )).when(handlePaymentResultUseCase).handle(
                "order-service-order-payment-saga",
                "order-worker",
                300L,
                sourceEvent
        );

        consumer.onMessage("raw-message", acknowledgment);

        verify(sagaDeadLetterPublisher).publish(
                "order.payment.saga.v1.dlq",
                "order.payment.saga.v1",
                "order-service-order-payment-saga",
                "PAYMENT_APPROVED_PAYLOAD_DESERIALIZATION_FAILED",
                "payload parse failed",
                "raw-message"
        );
        verify(acknowledgment).acknowledge();
    }
}