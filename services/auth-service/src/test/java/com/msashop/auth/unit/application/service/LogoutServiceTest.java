package com.msashop.auth.unit.application.service;

import com.msashop.auth.application.port.in.model.LogoutCommand;
import com.msashop.auth.application.port.out.RefreshTokenPort;
import com.msashop.auth.application.service.LogoutService;
import com.msashop.auth.application.service.token.RefreshTokenParser;
import com.msashop.auth.application.service.token.TokenHasher;
import com.msashop.common.web.exception.AuthErrorCode;
import com.msashop.common.web.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

    @Mock
    private RefreshTokenPort refreshTokenPort;

    @Mock
    private RefreshTokenParser refreshTokenParser;

    @Mock
    private TokenHasher tokenHasher;

    @InjectMocks
    private LogoutService service;

    @BeforeEach
    void setUp() {
        service = new LogoutService(refreshTokenPort, refreshTokenParser, tokenHasher);
    }

    @Test
    void should_ignore_logout_when_refresh_token_is_blank() {
        // refresh token이 없으면 멱등하게 종료한다.
        service.logout(new LogoutCommand(" "));

        verify(refreshTokenParser, never()).validate(any());
        verify(refreshTokenPort, never()).revoke(any());
    }

    @Test
    void should_ignore_logout_when_refresh_token_validation_fails() {
        // 형식이 잘못된 refresh token도 예외 없이 종료한다.
        when(refreshTokenParser.validate("bad-token"))
                .thenThrow(new BusinessException(AuthErrorCode.AUTH_REFRESH_INVALID_FORMAT));

        service.logout(new LogoutCommand("bad-token"));

        verify(tokenHasher, never()).sha256Hex(any());
        verify(refreshTokenPort, never()).revoke(any());
    }

    @Test
    void should_revoke_refresh_token_when_active_token_exists() {
        // 활성 refresh token이 존재하면 revoke 처리한다.
        when(refreshTokenParser.validate("rt")).thenReturn("rt");
        when(tokenHasher.sha256Hex("rt")).thenReturn("token-hash");
        when(refreshTokenPort.findActiveByTokenHash(any(), any())).thenReturn(Optional.of(
                new RefreshTokenPort.RefreshTokenRecord(1L, "token-hash", 1L, Instant.now().plusSeconds(300), false)
        ));

        service.logout(new LogoutCommand("rt"));

        verify(refreshTokenPort).revoke("token-hash");
    }
}
