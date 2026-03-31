package com.msashop.auth.unit.application.service;

import com.msashop.auth.application.port.in.model.LoginCommand;
import com.msashop.auth.application.port.in.model.LoginResult;
import com.msashop.auth.application.port.out.LoadUserPort;
import com.msashop.auth.application.port.out.RefreshTokenPort;
import com.msashop.auth.application.service.LoginService;
import com.msashop.auth.application.service.token.RefreshTokenGenerator;
import com.msashop.auth.application.service.token.TokenHasher;
import com.msashop.auth.application.service.token.TokenIssuer;
import com.msashop.auth.config.auth.RefreshTokenProperties;
import com.msashop.common.web.exception.AuthErrorCode;
import com.msashop.common.web.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenPort refreshTokenPort;

    @Mock
    private RefreshTokenGenerator refreshTokenGenerator;

    @Mock
    private TokenHasher tokenHasher;

    @Mock
    private TokenIssuer tokenIssuer;

    private LoginService service;

    @BeforeEach
    void setUp() {
        service = new LoginService(
                loadUserPort,
                passwordEncoder,
                refreshTokenPort,
                refreshTokenGenerator,
                tokenHasher,
                new RefreshTokenProperties(3600L, "refresh"),
                tokenIssuer
        );
    }

    @Test
    void should_throw_invalid_credentials_when_user_is_not_found() {
        // 로그인 아이디로 사용자를 찾지 못하면 인증 실패로 처리한다.
        when(loadUserPort.findByLoginId("tester")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.login(new LoginCommand("tester", "pw"))
        );

        assertEquals(AuthErrorCode.AUTH_INVALID_CREDENTIALS, ex.errorCode());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void should_throw_disabled_user_when_account_is_not_enabled() {
        // 비활성 계정이면 비밀번호 검증 전에 차단한다.
        when(loadUserPort.findByLoginId("tester")).thenReturn(Optional.of(user(false, List.of("ROLE_USER"))));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.login(new LoginCommand("tester", "pw"))
        );

        assertEquals(AuthErrorCode.AUTH_DISABLED_USER, ex.errorCode());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void should_throw_invalid_credentials_when_password_does_not_match() {
        // 비밀번호가 틀리면 access token이나 refresh token을 발급하지 않는다.
        when(loadUserPort.findByLoginId("tester")).thenReturn(Optional.of(user(true, List.of("ROLE_USER"))));
        when(passwordEncoder.matches("wrong-pw", "hashed-password")).thenReturn(false);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.login(new LoginCommand("tester", "wrong-pw"))
        );

        assertEquals(AuthErrorCode.AUTH_INVALID_CREDENTIALS, ex.errorCode());
        verify(tokenIssuer, never()).issueAccessToken(any(), any());
        verify(refreshTokenPort, never()).save(any());
    }

    @Test
    void should_issue_tokens_and_store_hashed_refresh_token_on_success() {
        // 로그인 성공 시 access token을 발급하고 refresh token 해시만 저장한다.
        when(loadUserPort.findByLoginId("tester")).thenReturn(Optional.of(user(true, List.of("ROLE_ADMIN"))));
        when(passwordEncoder.matches("plain-pw", "hashed-password")).thenReturn(true);
        when(tokenIssuer.issueAccessToken(1L, List.of("ROLE_ADMIN"))).thenReturn("access-token");
        when(refreshTokenGenerator.generate()).thenReturn(new RefreshTokenGenerator.GeneratedRefreshToken("refresh-raw"));
        when(tokenHasher.sha256Hex("refresh-raw")).thenReturn("refresh-hash");

        Instant before = Instant.now();
        LoginResult result = service.login(new LoginCommand("tester", "plain-pw"));
        Instant after = Instant.now();

        ArgumentCaptor<RefreshTokenPort.NewRefreshToken> captor = ArgumentCaptor.forClass(RefreshTokenPort.NewRefreshToken.class);
        verify(refreshTokenPort).save(captor.capture());

        RefreshTokenPort.NewRefreshToken saved = captor.getValue();
        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-raw", result.refreshToken());
        assertEquals("refresh-hash", saved.tokenHash());
        assertEquals(1L, saved.userId());
        assertTrue(saved.expiresAt().isAfter(before.plusSeconds(3598)));
        assertTrue(saved.expiresAt().isBefore(after.plusSeconds(3602)));
    }

    private LoadUserPort.AuthUserRecord user(boolean enabled, List<String> roles) {
        return new LoadUserPort.AuthUserRecord(
                1L,
                "tester@test.com",
                "tester",
                "hashed-password",
                enabled,
                roles
        );
    }
}
