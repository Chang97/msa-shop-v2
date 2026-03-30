package com.msashop.auth.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.auth.application.port.in.HandleAuthUserSagaCompletionUseCase;
import com.msashop.auth.application.port.out.CredentialPort;
import com.msashop.auth.application.port.out.ProcessedEventPort;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.InvalidSagaMessageException;
import com.msashop.common.event.SagaClaimExecutor;
import com.msashop.common.event.payload.UserProfileCreatedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthUserSagaCompletionService implements HandleAuthUserSagaCompletionUseCase {

    private final ObjectMapper objectMapper;
    private final ProcessedEventPort processedEventPort;
    private final CredentialPort credentialPort;

    @Override
    @Transactional
    public boolean handle(
            String consumerGroup,
            String workerId,
            long claimTimeoutSeconds,
            EventEnvelope envelope
    ) throws Exception {
        return SagaClaimExecutor.execute(
                claimTimeoutSeconds,
                (now, staleThreshold) -> processedEventPort.claim(
                        consumerGroup,
                        envelope,
                        workerId,
                        now,
                        staleThreshold
                ),
                () -> handleClaimedEvent(consumerGroup, envelope),
                lastError -> processedEventPort.releaseClaim(consumerGroup, envelope.eventId(), lastError)
        );
    }

    private boolean handleClaimedEvent(String consumerGroup, EventEnvelope envelope) {
        if (EventTypes.USER_PROFILE_CREATED.equals(envelope.eventType())) {
            UserProfileCreatedPayload payload = deserializeCreatedPayload(consumerGroup, envelope);
            credentialPort.enable(payload.authUserId());
            processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
            return true;
        }

        if (EventTypes.USER_PROFILE_CREATION_FAILED.equals(envelope.eventType())) {
            processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
            return true;
        }

        return true;
    }

    private UserProfileCreatedPayload deserializeCreatedPayload(String consumerGroup, EventEnvelope envelope) {
        try {
            return objectMapper.readValue(envelope.payloadJson(), UserProfileCreatedPayload.class);
        } catch (JsonProcessingException e) {
            processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
            throw new InvalidSagaMessageException(
                    "USER_PROFILE_CREATED_PAYLOAD_DESERIALIZATION_FAILED",
                    "UserProfileCreated payload 역직렬화에 실패했습니다.",
                    e
            );
        }
    }
}
