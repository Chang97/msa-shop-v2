package com.msashop.user.unit.application.service;

import com.msashop.common.event.EventEnvelope;
import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.user.application.event.UserSagaEventFactory;
import com.msashop.user.application.port.out.LoadUserPort;
import com.msashop.user.application.port.out.OutboxEventPort;
import com.msashop.user.application.port.out.SaveUserPort;
import com.msashop.user.application.service.DeactivateMeService;
import com.msashop.user.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeactivateMeServiceTest {

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private SaveUserPort saveUserPort;

    @Mock
    private OutboxEventPort outboxEventPort;

    @Mock
    private UserSagaEventFactory eventFactory;

    private DeactivateMeService service;

    @BeforeEach
    void setUp() {
        service = new DeactivateMeService(loadUserPort, saveUserPort, outboxEventPort, eventFactory);
    }

    @Test
    void should_throw_not_found_when_user_is_missing() {
        when(loadUserPort.findByAuthUserId(20L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.deactivateMe(20L));

        assertEquals(CommonErrorCode.COMMON_NOT_FOUND, ex.errorCode());
        verify(saveUserPort, never()).deactivate(any());
        verify(outboxEventPort, never()).append(any());
    }

    @Test
    void should_deactivate_user_and_append_user_deactivated_event_when_user_exists() {
        User user = activeUser();
        EventEnvelope event = new EventEnvelope(
                "event-1",
                "UserDeactivated",
                "USER_PROFILE",
                "20",
                "event-1",
                "event-1",
                "event-1",
                "user-service",
                "auth.user.saga.v1",
                "20",
                Instant.parse("2026-03-31T00:00:00Z"),
                "{\"authUserId\":20,\"userId\":10}"
        );

        when(loadUserPort.findByAuthUserId(20L)).thenReturn(Optional.of(user));
        when(eventFactory.userDeactivated(20L, 10L)).thenReturn(event);

        service.deactivateMe(20L);

        InOrder inOrder = inOrder(saveUserPort, eventFactory, outboxEventPort);
        inOrder.verify(saveUserPort).deactivate(user);
        inOrder.verify(eventFactory).userDeactivated(20L, 10L);
        inOrder.verify(outboxEventPort).append(event);
    }

    @Test
    void should_do_nothing_when_user_is_already_deactivated() {
        User user = inactiveUser();

        when(loadUserPort.findByAuthUserId(20L)).thenReturn(Optional.of(user));

        service.deactivateMe(20L);

        verify(saveUserPort, never()).deactivate(any());
        verify(eventFactory, never()).userDeactivated(any(), any());
        verify(outboxEventPort, never()).append(any());
    }

    private User activeUser() {
        return new User(
                10L,
                20L,
                "Hong Gil Dong",
                "EMP-001",
                "Platform Team",
                "010-1234-5678",
                true,
                Instant.parse("2026-03-30T00:00:00Z"),
                1L,
                Instant.parse("2026-03-31T00:00:00Z"),
                1L
        );
    }

    private User inactiveUser() {
        return new User(
                10L,
                20L,
                "Hong Gil Dong",
                "EMP-001",
                "Platform Team",
                "010-1234-5678",
                false,
                Instant.parse("2026-03-30T00:00:00Z"),
                1L,
                Instant.parse("2026-03-31T00:00:00Z"),
                1L
        );
    }
}
