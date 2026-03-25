package com.msashop.common.event;

import java.util.function.Predicate;

/**
 * saga consumer listener에서 반복되는 공통 흐름을 묶는다.
 *
 * Kafka, Jackson 타입에 직접 묶이지 않도록
 * 파싱 함수와 ack 동작을 호출부에서 주입받는다.
 */
public final class SagaConsumerSupport {

    private SagaConsumerSupport() {
    }

    public static void consume(
            String rawMessage,
            AckAction ackAction,
            ThrowingEnvelopeParser envelopeParser,
            String sagaTopic,
            String dlqTopic,
            String consumerGroup,
            Predicate<String> interestedEventType,
            ThrowingEnvelopeHandler handler,
            DeadLetterPublisherPort deadLetterPublisher
    ) throws Exception {
        EventEnvelope envelope;
        try {
            envelope = envelopeParser.parse(rawMessage);
        } catch (Exception e) {
            deadLetterPublisher.publish(
                    dlqTopic,
                    sagaTopic,
                    consumerGroup,
                    "EVENT_ENVELOPE_DESERIALIZATION_FAILED",
                    e.getMessage(),
                    rawMessage
            );
            ackAction.acknowledge();
            return;
        }

        // 여러 서비스가 같은 토픽을 공유하므로
        // 관심 없는 이벤트는 listener 본문에서 분기하지 않고 바로 ack 한다.
        if (!interestedEventType.test(envelope.eventType())) {
            ackAction.acknowledge();
            return;
        }

        try {
            boolean handled = handler.handle(envelope);
            if (handled) {
                ackAction.acknowledge();
            }
        } catch (InvalidSagaMessageException e) {
            // envelope 파싱은 성공했지만 비즈니스 payload가 깨진 경우다.
            // 이런 메시지는 재시도보다 DLQ 격리가 맞다.
            deadLetterPublisher.publish(
                    dlqTopic,
                    sagaTopic,
                    consumerGroup,
                    e.reasonCode(),
                    e.getMessage(),
                    rawMessage
            );
            ackAction.acknowledge();
        }
    }

    @FunctionalInterface
    public interface AckAction {
        void acknowledge();
    }

    @FunctionalInterface
    public interface ThrowingEnvelopeParser {
        EventEnvelope parse(String rawMessage) throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingEnvelopeHandler {
        boolean handle(EventEnvelope envelope) throws Exception;
    }
}
