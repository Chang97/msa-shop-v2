package com.msashop.auth.unit.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.auth.application.port.out.CredentialPort;
import com.msashop.auth.application.port.out.ProcessedEventPort;
import com.msashop.auth.application.service.AuthUserSagaCompletionService;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.payload.UserDeactivatedPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUserSagaCompletionServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProcessedEventPort processedEventPort;

    @Mock
    private CredentialPort credentialPort;

    @InjectMocks
    private AuthUserSagaCompletionService service;

    @Test
    void should_keep_disabled_when_user_profile_creation_failed_event_is_received() throws Exception {
        EventEnvelope failureEvent = new EventEnvelope(
                "event-1",
                EventTypes.USER_PROFILE_CREATION_FAILED,
                "USER_PROFILE",
                "1",
                "saga-1",
                "corr-1",
                "cause-1",
                "user-service",
                "auth.user.saga.v1",
                "1",
                Instant.now(),
                "{\"authUserId\":1,\"reasonCode\":\"USER_PROFILE_PERSISTENCE_FAILED\",\"reasonMessage\":\"forced failure\"}"
        );

        when(processedEventPort.claim(
                eq("auth-service-auth-user-saga"),
                eq(failureEvent),
                eq("auth-worker"),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(true);

        boolean handled = service.handle(
                "auth-service-auth-user-saga",
                "auth-worker",
                300L,
                failureEvent
        );

        assertTrue(handled);
        verify(credentialPort, never()).enable(any(Long.class));
        verify(credentialPort, never()).disable(any(Long.class));
        verify(processedEventPort).markProcessed(
                eq("auth-service-auth-user-saga"),
                eq("event-1"),
                any(Instant.class)
        );
        verify(processedEventPort, never()).releaseClaim(any(), any(), any());
    }

    @Test
    void should_disable_credential_when_user_deactivated_event_is_received() throws Exception {
        EventEnvelope deactivatedEvent = new EventEnvelope(
                "event-2",
                EventTypes.USER_DEACTIVATED,
                "USER_PROFILE",
                "1",
                "event-2",
                "event-2",
                "event-2",
                "user-service",
                "auth.user.saga.v1",
                "1",
                Instant.now(),
                "{\"authUserId\":1,\"userId\":10}"
        );

        when(processedEventPort.claim(
                eq("auth-service-auth-user-saga"),
                eq(deactivatedEvent),
                eq("auth-worker"),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(true);
        when(objectMapper.readValue(
                deactivatedEvent.payloadJson(),
                UserDeactivatedPayload.class
        )).thenReturn(new UserDeactivatedPayload(1L, 10L));

        boolean handled = service.handle(
                "auth-service-auth-user-saga",
                "auth-worker",
                300L,
                deactivatedEvent
        );

        assertTrue(handled);
        verify(credentialPort).disable(1L);
        verify(processedEventPort).markProcessed(
                eq("auth-service-auth-user-saga"),
                eq("event-2"),
                any(Instant.class)
        );
    }
}
