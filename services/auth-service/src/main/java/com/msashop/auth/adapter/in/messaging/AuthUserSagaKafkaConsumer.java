package com.msashop.auth.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.auth.application.port.out.CredentialPort;
import com.msashop.auth.application.port.out.ProcessedEventPort;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.payload.UserProfileCreatedPayload;
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
    private final CredentialPort credentialPort;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @KafkaListener(topics = "${app.kafka.topics.auth-user-saga}")
    public void onMessage(String rawMessage, Acknowledgment ack) throws Exception {
        EventEnvelope envelope = objectMapper.readValue(rawMessage, EventEnvelope.class);

        // auth-service가 관심 없는 이벤트는 읽고 버림
        if (!isAuthCompletionEvent(envelope.eventType())) {
            ack.acknowledge();
            return;
        }

        if (processedEventPort.exists(consumerGroup, envelope.eventId())) {
            ack.acknowledge();
            return;
        }

        if (EventTypes.USER_PROFILE_CREATED.equals(envelope.eventType())) {
            UserProfileCreatedPayload payload = objectMapper.readValue(envelope.payloadJson(), UserProfileCreatedPayload.class);

            // 프로필 생성이 끝났으므로 로그인 가능상태로 전환
            credentialPort.enable(payload.authUserId());
            processedEventPort.save(consumerGroup, envelope);
            ack.acknowledge();
            return;
        }

        if (EventTypes.USER_PROFILE_CREATION_FAILED.equals(envelope.eventType())) {
            // 초기 저장이 이미 disabled=false가 아니라 disabled 유지 상태이므로
            // 별도 삭제 보상 없이 처리 이력만 남긴다.
            processedEventPort.save(consumerGroup, envelope);
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
