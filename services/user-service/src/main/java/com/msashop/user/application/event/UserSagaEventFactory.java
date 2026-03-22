package com.msashop.user.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTopics;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.payload.UserProfileCreatedPayload;
import com.msashop.common.event.payload.UserProfileCreationFailedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * user-service가 saga 처리 결과를 auth-service에 돌려줄 때 사용하는 factory.
 * 이전 이벤트의 saga/correlation/causation 정보를 이어받는 역할도 여기서 담당한다.
 */
@Component
@RequiredArgsConstructor
public class UserSagaEventFactory {

    private final ObjectMapper objectMapper;

    public EventEnvelope userProfileCreated(EventEnvelope source, Long authUserId, Long userId) {
        return new EventEnvelope(
                UUID.randomUUID().toString(),
                EventTypes.USER_PROFILE_CREATED,
                "USER_PROFILE",
                String.valueOf(authUserId),
                source.sagaId(),
                // 동일 saga 체인을 묶기 위해 시작 이벤트의 correlationId를 그대로 이어받는다.
                source.correlationId(),
                // 바로 직전 원인 이벤트는 AuthUserCreated이다.
                source.eventId(),
                "user-service",
                EventTopics.AUTH_USER_SAGA_V1,
                String.valueOf(authUserId),
                Instant.now(),
                writeJson(new UserProfileCreatedPayload(authUserId, userId))
        );
    }

    public EventEnvelope userProfileCreationFailed(
            EventEnvelope source,
            Long authUserId,
            String reasonCode,
            String reasonMessage
    ) {
        return new EventEnvelope(
                UUID.randomUUID().toString(),
                EventTypes.USER_PROFILE_CREATION_FAILED,
                "USER_PROFILE",
                String.valueOf(authUserId),
                source.sagaId(),
                source.correlationId(),
                source.eventId(),
                "user-service",
                EventTopics.AUTH_USER_SAGA_V1,
                String.valueOf(authUserId),
                Instant.now(),
                writeJson(new UserProfileCreationFailedPayload(authUserId, reasonCode, reasonMessage))
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("이벤트 payload 직렬화에 실패했습니다.", e);
        }
    }
}
