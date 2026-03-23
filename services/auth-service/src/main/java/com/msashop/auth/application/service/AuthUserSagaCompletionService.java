package com.msashop.auth.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.auth.application.port.in.HandleAuthUserSagaCompletionUseCase;
import com.msashop.auth.application.port.out.CredentialPort;
import com.msashop.auth.application.port.out.ProcessedEventPort;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.InvalidSagaMessageException;
import com.msashop.common.event.payload.UserProfileCreatedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * auth-service가 회원가입 saga 완료 이벤트를 처리하는 application service.
 * listener에서 Kafka/Spring 의존성을 걷어내고,
 * 실제 비즈니스 처리와 processed_event 상태 전이를 한 곳에 모은다.
 */
@Service
@RequiredArgsConstructor
public class AuthUserSagaCompletionService implements HandleAuthUserSagaCompletionUseCase {
    private final ObjectMapper objectMapper;
    private final ProcessedEventPort processedEventPort;
    private final CredentialPort credentialPort;

    /**
     * auth-service가 관심 있는 완료 이벤트를 실제로 처리한다.
     *
     * @return true면 이 이벤트는 정상적으로 처리했거나 이미 다른 worker가 처리했으므로 ack 가능
     * @throws Exception 인프라 예외는 다시 던져 Kafka 재시도를 타게 한다
     */
    @Override
    @Transactional
    public boolean handle(
            String consumerGroup,
            String workerId,
            long claimTimeoutSeconds,
            EventEnvelope envelope
    ) throws Exception {
        Instant now = Instant.now();

        boolean claimed = processedEventPort.claim(
                consumerGroup,
                envelope,
                workerId,
                now,
                now.minusSeconds(claimTimeoutSeconds)
        );

        // 이미 다른 worker가 처리 중이거나 처리 완료했다면 현재 listener는 조용히 ack 하면 된다.
        if (!claimed) {
            return true;
        }

        try {
            if (EventTypes.USER_PROFILE_CREATED.equals(envelope.eventType())) {
                UserProfileCreatedPayload payload;
                try {
                    payload = objectMapper.readValue(envelope.payloadJson(), UserProfileCreatedPayload.class);
                } catch (JsonProcessingException e) {
                    processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
                    throw new InvalidSagaMessageException(
                            "USER_PROFILE_CREATED_PAYLOAD_DESERIALIZATION_FAILED",
                            "UserProfileCreated payload 역직렬화에 실패했습니다.",
                            e
                    );
                }

                // 프로필 생성이 성공했으므로 auth 계정을 활성화한다.
                credentialPort.enable(payload.authUserId());

                // 비즈니스 처리까지 끝났으므로 processed_event를 최종 완료로 바꾼다.
                processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
                return true;
            }

            if (EventTypes.USER_PROFILE_CREATION_FAILED.equals(envelope.eventType())) {
                // 실패 보상 전략은 delete가 아니라 disabled 유지다.
                // auth 쪽에서는 추가 비즈니스 처리 없이 처리 완료만 기록하면 된다.
                processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
                return true;
            }

            // auth-service가 관심 없는 이벤트는 listener 선단에서 걸러지므로
            // 여기까지 오면 사실상 방어 코드다.
            return true;
        } catch (InvalidSagaMessageException e) {
            // poison message는 이미 processed 처리 후 DLQ로 보낼 것이므로
            // claim을 다시 풀지 않고 그대로 상위로 전달한다.
            throw e;
        } catch (Exception e) {
            // 인프라 예외나 역직렬화 실패 등은 처리권을 다시 풀어줘야 재시도 가능하다.
            processedEventPort.releaseClaim(consumerGroup, envelope.eventId(), e.getMessage());
            throw e;
        }
    }
}
