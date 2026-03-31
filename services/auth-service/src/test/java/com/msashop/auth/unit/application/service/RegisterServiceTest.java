package com.msashop.auth.unit.application.service;

import com.msashop.auth.application.event.AuthUserSagaEventFactory;
import com.msashop.auth.application.port.in.model.RegisterCommand;
import com.msashop.auth.application.port.out.CredentialPort;
import com.msashop.auth.application.port.out.OutboxEventPort;
import com.msashop.auth.application.port.out.UserRolePort;
import com.msashop.auth.application.service.RegisterService;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.CommonErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @Mock
    private CredentialPort credentialPort;

    @Mock
    private UserRolePort userRolePort;

    @Mock
    private OutboxEventPort outboxEventPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthUserSagaEventFactory eventFactory;

    @InjectMocks
    private RegisterService service;

    @Test
    void should_throw_conflict_when_email_is_already_used() {
        // 이메일이 중복이면 credential 저장 전에 차단한다.
        RegisterCommand command = command();
        when(credentialPort.existsByEmail(command.email())).thenReturn(true);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.register(command)
        );

        assertEquals(CommonErrorCode.COMMON_CONFLICT, ex.errorCode());
        verify(credentialPort, never()).saveDisabledCredential(any(), any(), any());
    }

    @Test
    void should_throw_conflict_when_login_id_is_already_used() {
        // 로그인 아이디가 중복이면 권한 부여와 outbox 적재를 진행하지 않는다.
        RegisterCommand command = command();
        when(credentialPort.existsByEmail(command.email())).thenReturn(false);
        when(credentialPort.existsByLoginId(command.loginId())).thenReturn(true);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.register(command)
        );

        assertEquals(CommonErrorCode.COMMON_CONFLICT, ex.errorCode());
        verify(userRolePort, never()).assignRole(any(), any());
        verify(outboxEventPort, never()).append(any());
    }

    @Test
    void should_save_disabled_credential_assign_default_role_and_append_outbox_event() {
        // 회원가입 시작 시 비활성 credential 저장, 기본 권한 부여, outbox 적재를 한 트랜잭션으로 묶는다.
        RegisterCommand command = command();
        EventEnvelope envelope = new EventEnvelope(
                "event-1",
                "AUTH_USER_CREATED",
                "AUTH_USER",
                "1",
                "saga-1",
                "corr-1",
                null,
                "auth-service",
                "auth.user.saga.v1",
                "1",
                null,
                "{}"
        );

        when(credentialPort.existsByEmail(command.email())).thenReturn(false);
        when(credentialPort.existsByLoginId(command.loginId())).thenReturn(false);
        when(passwordEncoder.encode(command.rawPassword())).thenReturn("encoded-password");
        when(credentialPort.saveDisabledCredential(command.email(), command.loginId(), "encoded-password")).thenReturn(1L);
        when(eventFactory.authUserCreated(any(), eq(1L), eq(command))).thenReturn(envelope);

        Long authUserId = service.register(command);

        assertEquals(1L, authUserId);
        verify(userRolePort).assignRole(1L, "ROLE_USER");
        verify(outboxEventPort).append(envelope);
    }

    private RegisterCommand command() {
        return new RegisterCommand(
                "tester@test.com",
                "tester",
                "plain-password",
                "테스터",
                "E001",
                "개발자",
                "010-1234-5678"
        );
    }
}
