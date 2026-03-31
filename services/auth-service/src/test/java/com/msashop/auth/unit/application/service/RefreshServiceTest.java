package com.msashop.auth.unit.application.service;

import com.msashop.auth.application.port.in.model.RefreshCommand;
import com.msashop.auth.application.port.in.model.RefreshResult;
import com.msashop.auth.application.port.out.LoadUserPort;
import com.msashop.auth.application.port.out.RefreshTokenPort;
import com.msashop.auth.application.service.RefreshService;
import com.msashop.auth.application.service.token.RefreshTokenGenerator;
import com.msashop.auth.application.service.token.RefreshTokenParser;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshServiceTest {

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private RefreshTokenPort refreshTokenPort;

    @Mock
    private RefreshTokenParser refreshTokenParser;

    @Mock
    private RefreshTokenGenerator refreshTokenGenerator;

    @Mock
    private TokenHasher tokenHasher;

    @Mock
    private TokenIssuer tokenIssuer;

    private RefreshService service;

    @BeforeEach
    void setUp() {
        service = new RefreshService(
                loadUserPort,
                refreshTokenPort,
                refreshTokenParser,
                refreshTokenGenerator,
                tokenHasher,
                new RefreshTokenProperties(3600L, "refresh"),
                tokenIssuer
        );
    }

    @Test
    void should_throw_refresh_invalid_when_token_does_not_exist_in_storage() {
        // 저장소에 없는 refresh token은 invalid로 처리한다.
        when(refreshTokenParser.validate("rt")).thenReturn("rt");
        when(tokenHasher.sha256Hex("rt")).thenReturn("old-hash");
        when(refreshTokenPort.findActiveByTokenHash(any(), any())).thenReturn(Optional.empty());
        when(refreshTokenPort.findByTokenHash("old-hash")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.refresh(new RefreshCommand("rt"))
        );

        assertEquals(AuthErrorCode.AUTH_REFRESH_INVALID, ex.errorCode());
    }

    @Test
    void should_throw_refresh_revoked_when_token_was_already_revoked() {
        // 이미 revoke된 refresh token은 재발급 대상이 아니다.
        when(refreshTokenParser.validate("rt")).thenReturn("rt");
        when(tokenHasher.sha256Hex("rt")).thenReturn("old-hash");
        when(refreshTokenPort.findActiveByTokenHash(any(), any())).thenReturn(Optional.empty());
        when(refreshTokenPort.findByTokenHash("old-hash")).thenReturn(Optional.of(
                new RefreshTokenPort.RefreshTokenRecord(10L, "old-hash", 1L, Instant.now().plusSeconds(600), true)
        ));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.refresh(new RefreshCommand("rt"))
        );

        assertEquals(AuthErrorCode.AUTH_REFRESH_REVOKED, ex.errorCode());
    }

    @Test
    void should_rotate_refresh_token_and_issue_access_token_with_current_roles() {
        // 성공 시 기존 refresh token은 revoke하고 현재 권한으로 새 access token을 발급한다.
        when(refreshTokenParser.validate("rt")).thenReturn("rt");
        when(tokenHasher.sha256Hex("rt")).thenReturn("old-hash");
        when(refreshTokenPort.findActiveByTokenHash(any(), any())).thenReturn(Optional.of(
                new RefreshTokenPort.RefreshTokenRecord(10L, "old-hash", 1L, Instant.now().plusSeconds(600), false)
        ));
        when(loadUserPort.findByUserId(1L)).thenReturn(Optional.of(
                new LoadUserPort.AuthUserRecord(1L, "tester@test.com", "tester", "hash", true, List.of("ROLE_ADMIN"))
        ));
        when(refreshTokenGenerator.generate()).thenReturn(new RefreshTokenGenerator.GeneratedRefreshToken("new-raw"));
        when(tokenHasher.sha256Hex("new-raw")).thenReturn("new-hash");
        when(tokenIssuer.issueAccessToken(1L, List.of("ROLE_ADMIN"))).thenReturn("new-access");

        Instant before = Instant.now();
        RefreshResult result = service.refresh(new RefreshCommand("rt"));
        Instant after = Instant.now();

        ArgumentCaptor<RefreshTokenPort.NewRefreshToken> captor = ArgumentCaptor.forClass(RefreshTokenPort.NewRefreshToken.class);
        verify(refreshTokenPort).revoke("old-hash");
        verify(refreshTokenPort).save(captor.capture());
        verify(tokenIssuer).issueAccessToken(1L, List.of("ROLE_ADMIN"));

        RefreshTokenPort.NewRefreshToken saved = captor.getValue();
        assertEquals("new-access", result.accessToken());
        assertEquals("new-raw", result.refreshToken());
        assertEquals("new-hash", saved.tokenHash());
        assertEquals(1L, saved.userId());
        assertTrue(saved.expiresAt().isAfter(before.plusSeconds(3598)));
        assertTrue(saved.expiresAt().isBefore(after.plusSeconds(3602)));
    }

    @Test
    void should_throw_disabled_user_when_owner_account_is_disabled() {
        // refresh 시점에 사용자가 비활성화되었으면 토큰을 재발급하지 않는다.
        when(refreshTokenParser.validate("rt")).thenReturn("rt");
        when(tokenHasher.sha256Hex("rt")).thenReturn("old-hash");
        when(refreshTokenPort.findActiveByTokenHash(any(), any())).thenReturn(Optional.of(
                new RefreshTokenPort.RefreshTokenRecord(10L, "old-hash", 1L, Instant.now().plusSeconds(600), false)
        ));
        when(loadUserPort.findByUserId(1L)).thenReturn(Optional.of(
                new LoadUserPort.AuthUserRecord(1L, "tester@test.com", "tester", "hash", false, List.of("ROLE_USER"))
        ));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.refresh(new RefreshCommand("rt"))
        );

        assertEquals(AuthErrorCode.AUTH_DISABLED_USER, ex.errorCode());
        verify(refreshTokenGenerator, never()).generate();
        verify(refreshTokenPort, never()).revoke(any());
    }
}
