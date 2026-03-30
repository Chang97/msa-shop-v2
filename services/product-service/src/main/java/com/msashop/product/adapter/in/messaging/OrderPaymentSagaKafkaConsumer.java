package com.msashop.product.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.SagaConsumerSupport;
import com.msashop.product.adapter.out.messaging.SagaDeadLetterPublisher;
import com.msashop.product.application.port.in.HandleOrderPaymentSagaUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka.consumers", name = "order-payment-saga-enabled", havingValue = "true")
public class OrderPaymentSagaKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final HandleOrderPaymentSagaUseCase handleOrderPaymentSagaUseCase;
    private final SagaDeadLetterPublisher sagaDeadLetterPublisher;

    @Value("${app.kafka.consumers.worker-id:${spring.application.name}}")
    private String workerId;

    @Value("${app.kafka.consumers.claim-timeout-seconds:300}")
    private long claimTimeoutSeconds;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @Value("${app.kafka.topics.order-payment-saga}")
    private String sagaTopic;

    @Value("${app.kafka.topics.order-payment-saga-dlq}")
    private String dlqTopic;

    @KafkaListener(topics = "${app.kafka.topics.order-payment-saga}")
    public void onMessage(String rawMessage, Acknowledgment ack) throws Exception {
        SagaConsumerSupport.consume(
                rawMessage,
                ack::acknowledge,
                this::parseEnvelope,
                consumerSpec(),
                envelope -> handleOrderPaymentSagaUseCase.handle(
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
                eventType -> EventTypes.STOCK_RESERVATION_REQUESTED.equals(eventType)
                        || EventTypes.PAYMENT_APPROVED.equals(eventType)
                        || EventTypes.PAYMENT_FAILED.equals(eventType)
        );
    }
}
