package com.msashop.user.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.InvalidSagaMessageException;
import com.msashop.common.event.payload.AuthUserCreatedPayload;
import com.msashop.common.web.exception.BusinessException;
import com.msashop.user.application.event.UserSagaEventFactory;
import com.msashop.user.application.port.in.HandleAuthUserCreatedSagaUseCase;
import com.msashop.user.application.port.in.ProvisionUserProfileUseCase;
import com.msashop.user.application.port.in.model.ProvisionUserProfileCommand;
import com.msashop.user.application.port.out.OutboxEventPort;
import com.msashop.user.application.port.out.ProcessedEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * user-service가 AuthUserCreated saga 시작 이벤트를 처리하는 application service.
 * 프로필 생성과 결과 이벤트 적재를 같은 트랜잭션 경계 안에서 묶는다.
 */
@Service
@RequiredArgsConstructor
public class AuthUserCreatedSagaService implements HandleAuthUserCreatedSagaUseCase {
    private final ObjectMapper objectMapper;
    private final ProcessedEventPort processedEventPort;
    private final ProvisionUserProfileUseCase provisionUserProfileUseCase;
    private final OutboxEventPort outboxEventPort;
    private final UserSagaEventFactory eventFactory;

    /**
     * user-service가 회원가입 시작 이벤트를 실제로 처리한다.
     *
     * @return true면 현재 listener는 ack 가능
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

        // 이미 다른 worker가 처리 중이거나 완료한 이벤트면 현재 listener는 ack만 하면 된다.
        if (!claimed) {
            return true;
        }

        AuthUserCreatedPayload payload;
        try {
            payload = objectMapper.readValue(envelope.payloadJson(), AuthUserCreatedPayload.class);
        } catch (JsonProcessingException e) {
            processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
            throw new InvalidSagaMessageException(
                    "AUTH_USER_CREATED_PAYLOAD_DESERIALIZATION_FAILED",
                    "AuthUserCreated payload 역직렬화에 실패했습니다.",
                    e
            );
        }

        try {
            Long createdUserId = provisionUserProfileUseCase.provision(
                    new ProvisionUserProfileCommand(
                            payload.authUserId(),
                            payload.userName(),
                            payload.empNo(),
                            payload.pstnName(),
                            payload.tel()
                    )
            );

            // 프로필 생성 성공 결과를 outbox에 적재한다.
            outboxEventPort.append(
                    eventFactory.userProfileCreated(envelope, payload.authUserId(), createdUserId)
            );

            // 성공 결과 이벤트 적재까지 끝났으므로 원본 이벤트 처리 완료로 기록한다.
            processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
            return true;
        } catch (BusinessException e) {
            // 비즈니스 실패는 saga 실패 이벤트를 만들고 정상 종료한다.
            // 이 경우는 재시도보다는 "실패 결과를 발행"하는 것이 목적이다.
            outboxEventPort.append(
                    eventFactory.userProfileCreationFailed(
                            envelope,
                            payload.authUserId(),
                            "USER_PROFILE_PERSISTENCE_FAILED",
                            e.getMessage()
                    )
            );

            processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
            return true;
        } catch (InvalidSagaMessageException e) {
            // poison message는 이미 processed 처리 후 DLQ로 보낼 것이므로
            // claim을 다시 풀지 않고 그대로 상위로 전달한다.
            throw e;
        } catch (Exception e) {
            // 인프라 예외는 처리권을 다시 풀어주고 재시도를 타게 한다.
            processedEventPort.releaseClaim(consumerGroup, envelope.eventId(), e.getMessage());
            throw e;
        }
    }
}
