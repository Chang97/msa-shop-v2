package com.msashop.user.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.InvalidSagaMessageException;
import com.msashop.common.event.SagaClaimExecutor;
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

@Service
@RequiredArgsConstructor
public class AuthUserCreatedSagaService implements HandleAuthUserCreatedSagaUseCase {

    private final ObjectMapper objectMapper;
    private final ProcessedEventPort processedEventPort;
    private final ProvisionUserProfileUseCase provisionUserProfileUseCase;
    private final OutboxEventPort outboxEventPort;
    private final UserSagaEventFactory eventFactory;

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
        AuthUserCreatedPayload payload = deserializePayload(consumerGroup, envelope);

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

            outboxEventPort.append(
                    eventFactory.userProfileCreated(envelope, payload.authUserId(), createdUserId)
            );
            processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
            return true;
        } catch (BusinessException e) {
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
        }
    }

    private AuthUserCreatedPayload deserializePayload(String consumerGroup, EventEnvelope envelope) {
        try {
            return objectMapper.readValue(envelope.payloadJson(), AuthUserCreatedPayload.class);
        } catch (JsonProcessingException e) {
            processedEventPort.markProcessed(consumerGroup, envelope.eventId(), Instant.now());
            throw new InvalidSagaMessageException(
                    "AUTH_USER_CREATED_PAYLOAD_DESERIALIZATION_FAILED",
                    "AuthUserCreated payload deserialization failed.",
                    e
            );
        }
    }
}
