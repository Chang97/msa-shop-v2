package com.msashop.user.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTopics;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.payload.UserDeactivatedPayload;
import com.msashop.common.event.payload.UserProfileCreatedPayload;
import com.msashop.common.event.payload.UserProfileCreationFailedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserSagaEventFactory {

    private final ObjectMapper objectMapper;

    public EventEnvelope userDeactivated(Long authUserId, Long userId) {
        String eventId = UUID.randomUUID().toString();
        return new EventEnvelope(
                eventId,
                EventTypes.USER_DEACTIVATED,
                "USER_PROFILE",
                String.valueOf(authUserId),
                eventId,
                eventId,
                eventId,
                "user-service",
                EventTopics.AUTH_USER_SAGA_V1,
                String.valueOf(authUserId),
                Instant.now(),
                writeJson(new UserDeactivatedPayload(authUserId, userId))
        );
    }

    public EventEnvelope userProfileCreated(EventEnvelope source, Long authUserId, Long userId) {
        return new EventEnvelope(
                UUID.randomUUID().toString(),
                EventTypes.USER_PROFILE_CREATED,
                "USER_PROFILE",
                String.valueOf(authUserId),
                source.sagaId(),
                source.correlationId(),
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
