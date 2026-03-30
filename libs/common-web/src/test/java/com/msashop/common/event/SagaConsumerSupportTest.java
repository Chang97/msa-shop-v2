package com.msashop.common.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class SagaConsumerSupportTest {

    /**
     * envelope 파싱이 깨지면 DLQ 발행 후 즉시 ack 하는지 검증한다.
     */
    @Test
    void publishesDlqAndAcknowledgesWhenEnvelopeParsingFails() throws Exception {
        RecordingDeadLetterPublisher publisher = new RecordingDeadLetterPublisher();
        AtomicBoolean acknowledged = new AtomicBoolean();

        SagaConsumerSupport.consume(
                "raw-message",
                () -> acknowledged.set(true),
                raw -> {
                    throw new IllegalArgumentException("bad json");
                },
                new SagaConsumerSupport.ConsumerSpec("saga-topic", "dlq-topic", "group-a", eventType -> true),
                envelope -> true,
                publisher
        );

        assertThat(acknowledged).isTrue();
        assertThat(publisher.invocationCount).hasValue(1);
        assertThat(publisher.reasonCode).isEqualTo("EVENT_ENVELOPE_DESERIALIZATION_FAILED");
        assertThat(publisher.originalMessage).isEqualTo("raw-message");
    }

    /**
     * 관심 없는 이벤트 타입이면 handler를 타지 않고 바로 ack 하는지 검증한다.
     */
    @Test
    void acknowledgesImmediatelyWhenEventTypeIsNotRelevant() throws Exception {
        RecordingDeadLetterPublisher publisher = new RecordingDeadLetterPublisher();
        AtomicBoolean acknowledged = new AtomicBoolean();
        AtomicBoolean handled = new AtomicBoolean();

        SagaConsumerSupport.consume(
                "raw-message",
                () -> acknowledged.set(true),
                raw -> envelope("OTHER_EVENT"),
                new SagaConsumerSupport.ConsumerSpec("saga-topic", "dlq-topic", "group-a", "PAYMENT_APPROVED"::equals),
                envelope -> {
                    handled.set(true);
                    return true;
                },
                publisher
        );

        assertThat(acknowledged).isTrue();
        assertThat(handled).isFalse();
        assertThat(publisher.invocationCount).hasValue(0);
    }

    /**
     * payload 검증 단계에서 poison message로 판단되면 DLQ 발행 후 ack 하는지 검증한다.
     */
    @Test
    void publishesDlqAndAcknowledgesWhenPayloadIsInvalid() throws Exception {
        RecordingDeadLetterPublisher publisher = new RecordingDeadLetterPublisher();
        AtomicBoolean acknowledged = new AtomicBoolean();

        SagaConsumerSupport.consume(
                "raw-message",
                () -> acknowledged.set(true),
                raw -> envelope("PAYMENT_APPROVED"),
                new SagaConsumerSupport.ConsumerSpec("saga-topic", "dlq-topic", "group-a", "PAYMENT_APPROVED"::equals),
                envelope -> {
                    throw new InvalidSagaMessageException("INVALID_PAYLOAD", "payload broken", null);
                },
                publisher
        );

        assertThat(acknowledged).isTrue();
        assertThat(publisher.invocationCount).hasValue(1);
        assertThat(publisher.reasonCode).isEqualTo("INVALID_PAYLOAD");
    }

    /**
     * handler가 정상 처리되면 DLQ 없이 ack 하는지 검증한다.
     */
    @Test
    void acknowledgesWhenHandlerCompletesSuccessfully() throws Exception {
        RecordingDeadLetterPublisher publisher = new RecordingDeadLetterPublisher();
        AtomicBoolean acknowledged = new AtomicBoolean();

        SagaConsumerSupport.consume(
                "raw-message",
                () -> acknowledged.set(true),
                raw -> envelope("PAYMENT_APPROVED"),
                new SagaConsumerSupport.ConsumerSpec("saga-topic", "dlq-topic", "group-a", "PAYMENT_APPROVED"::equals),
                envelope -> true,
                publisher
        );

        assertThat(acknowledged).isTrue();
        assertThat(publisher.invocationCount).hasValue(0);
    }

    /**
     * 테스트에서 공통으로 사용하는 최소 envelope를 생성한다.
     */
    private EventEnvelope envelope(String eventType) {
        return new EventEnvelope(
                "event-1",
                eventType,
                "Order",
                "order-1",
                "saga-1",
                "corr-1",
                "cause-1",
                "order-service",
                "saga-topic",
                "key-1",
                Instant.now(),
                "{}"
        );
    }

    /**
     * DLQ 발행 결과를 메모리에 기록하는 테스트용 publisher다.
     */
    private static final class RecordingDeadLetterPublisher implements DeadLetterPublisherPort {
        private final AtomicInteger invocationCount = new AtomicInteger();
        private String reasonCode;
        private String originalMessage;

        /**
         * 마지막 발행 결과만 저장해서 검증에 사용한다.
         */
        @Override
        public void publish(
                String dlqTopic,
                String originalTopic,
                String consumerGroup,
                String reasonCode,
                String reasonMessage,
                String originalMessage
        ) {
            invocationCount.incrementAndGet();
            this.reasonCode = reasonCode;
            this.originalMessage = originalMessage;
        }
    }
}
