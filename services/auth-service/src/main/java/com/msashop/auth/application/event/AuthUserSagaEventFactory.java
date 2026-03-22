package com.msashop.auth.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.auth.application.port.in.model.RegisterCommand;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTopics;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.payload.AuthUserCreatedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * auth-service가 발행하는 saga 이벤트를 한 곳에서 생성한다.
 * 이벤트 구조를 서비스 곳곳에 흩뿌리지 않기 위해 factory로 모은다.
 */
@Component
@RequiredArgsConstructor
public class AuthUserSagaEventFactory {

    private final ObjectMapper objectMapper;

    public EventEnvelope authUserCreated(String sagaId, Long authUserId, RegisterCommand command) {
        AuthUserCreatedPayload payload = new AuthUserCreatedPayload(
                authUserId,
                command.userName(),
                command.empNo(),
                command.pstnName(),
                command.tel()
        );

        String eventId = UUID.randomUUID().toString();

        return new EventEnvelope(
                eventId,
                EventTypes.AUTH_USER_CREATED,
                "AUTH_USER",
                String.valueOf(authUserId),
                sagaId,
                // saga 시작 이벤트이므로 correlationId는 단순하게 자기 eventId로 둠.
                eventId,
                // 첫 이벤트이기에 causationId는 없음
                null,
                "auth-service",
                EventTopics.AUTH_USER_SAGA_V1,
                String.valueOf(authUserId),
                Instant.now(),
                writeJson(payload)
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("이벤트 payload 직렬화에 실패하였습니다.", e);
        }
    }
}
