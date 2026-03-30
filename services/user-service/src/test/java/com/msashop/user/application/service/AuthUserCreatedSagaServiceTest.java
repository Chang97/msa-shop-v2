package com.msashop.user.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.payload.AuthUserCreatedPayload;
import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.user.application.event.UserSagaEventFactory;
import com.msashop.user.application.port.in.ProvisionUserProfileUseCase;
import com.msashop.user.application.port.out.OutboxEventPort;
import com.msashop.user.application.port.out.ProcessedEventPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUserCreatedSagaServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProcessedEventPort processedEventPort;

    @Mock
    private ProvisionUserProfileUseCase provisionUserProfileUseCase;

    @Mock
    private OutboxEventPort outboxEventPort;

    @Mock
    private UserSagaEventFactory eventFactory;

    @InjectMocks
    private AuthUserCreatedSagaService service;

    @Test
    void should_append_failure_event_when_profile_creation_fails() throws Exception {
        EventEnvelope sourceEvent = new EventEnvelope(
                "event-1",
                EventTypes.AUTH_USER_CREATED,
                "AUTH_USER",
                "1",
                "saga-1",
                "corr-1",
                null,
                "auth-service",
                "auth.user.saga.v1",
                "1",
                Instant.now(),
                "{\"authUserId\":1}"
        );

        AuthUserCreatedPayload payload = new AuthUserCreatedPayload(
                1L,
                "Hong",
                "EMP-1",
                "QA",
                "010-1111-2222"
        );

        EventEnvelope failureEvent = new EventEnvelope(
                "event-2",
                EventTypes.USER_PROFILE_CREATION_FAILED,
                "USER_PROFILE",
                "1",
                "saga-1",
                "corr-1",
                "event-1",
                "user-service",
                "auth.user.saga.v1",
                "1",
                Instant.now(),
                "{\"authUserId\":1,\"reasonCode\":\"USER_PROFILE_PERSISTENCE_FAILED\",\"reasonMessage\":\"forced failure\"}"
        );

        when(processedEventPort.claim(
                eq("user-service-auth-user-saga"),
                eq(sourceEvent),
                eq("user-worker"),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(true);
        when(objectMapper.readValue(sourceEvent.payloadJson(), AuthUserCreatedPayload.class)).thenReturn(payload);
        doThrow(new BusinessException(CommonErrorCode.COMMON_VALIDATION, "forced failure"))
                .when(provisionUserProfileUseCase)
                .provision(any());
        when(eventFactory.userProfileCreationFailed(
                sourceEvent,
                1L,
                "USER_PROFILE_PERSISTENCE_FAILED",
                "forced failure"
        )).thenReturn(failureEvent);

        boolean handled = service.handle(
                "user-service-auth-user-saga",
                "user-worker",
                300L,
                sourceEvent
        );

        assertTrue(handled);
        verify(outboxEventPort).append(failureEvent);
        verify(processedEventPort).markProcessed(
                eq("user-service-auth-user-saga"),
                eq("event-1"),
                any(Instant.class)
        );
        verify(processedEventPort, never()).releaseClaim(any(), any(), any());
    }
}
