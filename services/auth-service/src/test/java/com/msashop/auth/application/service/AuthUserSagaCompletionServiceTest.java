package com.msashop.auth.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.auth.application.port.out.CredentialPort;
import com.msashop.auth.application.port.out.ProcessedEventPort;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
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
        verify(processedEventPort).markProcessed(
                eq("auth-service-auth-user-saga"),
                eq("event-1"),
                any(Instant.class)
        );
        verify(processedEventPort, never()).releaseClaim(any(), any(), any());
    }
}
