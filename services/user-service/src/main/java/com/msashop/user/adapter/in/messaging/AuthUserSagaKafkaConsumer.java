package com.msashop.user.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.payload.AuthUserCreatedPayload;
import com.msashop.common.web.exception.BusinessException;
import com.msashop.user.application.event.UserSagaEventFactory;
import com.msashop.user.application.port.in.ProvisionUserProfileUseCase;
import com.msashop.user.application.port.in.model.ProvisionUserProfileCommand;
import com.msashop.user.application.port.out.OutboxEventPort;
import com.msashop.user.application.port.out.ProcessedEventPort;
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
    private final ProcessedEventPort processedEventPort;
    private final ProvisionUserProfileUseCase provisionUserProfileUseCase;
    private final OutboxEventPort outboxEventPort;
    private final UserSagaEventFactory eventFactory;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @KafkaListener(topics = "${app.kafka.topics.auth-user-saga}")
    public void onMessage(String rawMessage, Acknowledgment ack) throws Exception {
        EventEnvelope envelope = objectMapper.readValue(rawMessage, EventEnvelope.class);

        // user-service가 처리할 이벤트가 아니면 ack 하고 넘긴다.
        if (!EventTypes.AUTH_USER_CREATED.equals(envelope.eventType())) {
            ack.acknowledge();
            return;
        }

        // 이미 처리한 eventId면 중복 소비이므로 비즈니스 로직 없이 ack 한다.
        if (processedEventPort.exists(consumerGroup, envelope.eventId())) {
            ack.acknowledge();
            return;
        }

        // envelope.payloadJson 안의 실제 비즈니스 payload를 역직렬화한다.
        AuthUserCreatedPayload payload = objectMapper.readValue(envelope.payloadJson(), AuthUserCreatedPayload.class);

        try {
            Long createdUserId = provisionUserProfileUseCase.provision(new ProvisionUserProfileCommand(
                    payload.authUserId(),
                    payload.userName(),
                    payload.empNo(),
                    payload.pstnName(),
                    payload.tel()
            ));

            // 성공 결과 이벤트를 같은 로컬 트랜잭션 안에서 outbox에 적재한다.
            outboxEventPort.append(eventFactory.userProfileCreated(envelope, payload.authUserId(), createdUserId));

            // 처리 이력 저장 후 ack
            processedEventPort.save(consumerGroup, envelope);
            ack.acknowledge();
            return;
        } catch (BusinessException e) {
            // 비즈니스 실패는 사가 실패로 간주하고 실패 이벤트를 발행
            outboxEventPort.append(eventFactory.userProfileCreationFailed(
                    envelope,
                    payload.authUserId(),
                    "USER_PROFILE_PERSISTENCE_FAILED",
                    e.getMessage()
            ));
            processedEventPort.save(consumerGroup, envelope);
            ack.acknowledge();
            return;
        }
    }
}
