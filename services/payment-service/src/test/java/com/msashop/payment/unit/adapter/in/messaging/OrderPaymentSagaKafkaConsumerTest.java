package com.msashop.payment.unit.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.InvalidSagaMessageException;
import com.msashop.payment.adapter.in.messaging.OrderPaymentSagaKafkaConsumer;
import com.msashop.payment.adapter.out.messaging.SagaDeadLetterPublisher;
import com.msashop.payment.application.port.in.HandleOrderPaymentRequestedUseCase;
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

/**
 * payment-service Kafka consumer 테스트.
 *
 * 검증 목적:
 * - listener가 비즈니스 로직을 직접 처리하지 않고
 * - InvalidSagaMessageException 발생 시 DLQ publish + ack만 수행하는지 확인한다
 *
 * 즉, listener 책임만 얇게 검증하는 테스트다.
 */
@ExtendWith(MockitoExtension.class)
class OrderPaymentSagaKafkaConsumerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HandleOrderPaymentRequestedUseCase handleOrderPaymentRequestedUseCase;

    @Mock
    private SagaDeadLetterPublisher sagaDeadLetterPublisher;

    @Mock
    private Acknowledgment acknowledgment;

    private OrderPaymentSagaKafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OrderPaymentSagaKafkaConsumer(
                objectMapper,
                handleOrderPaymentRequestedUseCase,
                sagaDeadLetterPublisher
        );

        ReflectionTestUtils.setField(consumer, "consumerGroup", "payment-service-order-payment-saga");
        ReflectionTestUtils.setField(consumer, "workerId", "payment-worker");
        ReflectionTestUtils.setField(consumer, "claimTimeoutSeconds", 300L);
        ReflectionTestUtils.setField(consumer, "sagaTopic", "order.payment.saga.v1");
        ReflectionTestUtils.setField(consumer, "dlqTopic", "order.payment.saga.v1.dlq");
    }

    /**
     * use case가 InvalidSagaMessageException을 던지는 케이스.
     *
     * 기대값:
     * - 원본 raw message를 DLQ로 발행한다
     * - Kafka 메시지는 ack 처리한다
     *
     * 의미:
     * - payload가 깨진 poison message는 재시도보다 격리가 맞다
     */
    @Test
    void should_publish_to_dlq_and_ack_when_use_case_throws_invalid_saga_message() throws Exception {
        EventEnvelope sourceEvent = new EventEnvelope(
                "event-1",
                EventTypes.STOCK_RESERVED,
                "STOCK_RESERVATION",
                "reservation-1",
                "saga-1",
                "corr-1",
                "cause-1",
                "product-service",
                "order.payment.saga.v1",
                "1",
                Instant.now(),
                "{\"orderId\":1}"
        );

        when(objectMapper.readValue("raw-message", EventEnvelope.class)).thenReturn(sourceEvent);
        doThrow(new InvalidSagaMessageException(
                "STOCK_RESERVED_PAYLOAD_DESERIALIZATION_FAILED",
                "payload parse failed",
                new RuntimeException("bad payload")
        )).when(handleOrderPaymentRequestedUseCase).handle(
                "payment-service-order-payment-saga",
                "payment-worker",
                300L,
                sourceEvent
        );

        consumer.onMessage("raw-message", acknowledgment);

        verify(sagaDeadLetterPublisher).publish(
                "order.payment.saga.v1.dlq",
                "order.payment.saga.v1",
                "payment-service-order-payment-saga",
                "STOCK_RESERVED_PAYLOAD_DESERIALIZATION_FAILED",
                "payload parse failed",
                "raw-message"
        );
        verify(acknowledgment).acknowledge();
    }
}