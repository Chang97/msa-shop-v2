package com.msashop.auth.application.service;

import com.msashop.auth.application.port.in.RefreshUseCase;
import com.msashop.auth.application.port.in.model.RefreshCommand;
import com.msashop.auth.application.port.in.model.RefreshResult;
import com.msashop.auth.application.port.out.LoadUserPort;
import com.msashop.auth.application.port.out.RefreshTokenPort;
import com.msashop.auth.application.service.token.RefreshTokenGenerator;
import com.msashop.auth.application.service.token.RefreshTokenParser;
import com.msashop.auth.application.service.token.TokenHasher;
import com.msashop.auth.application.service.token.TokenIssuer;
import com.msashop.auth.config.auth.RefreshTokenProperties;
import com.msashop.common.web.exception.AuthErrorCode;
import com.msashop.common.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * refresh token을 검증하고 access token과 refresh token을 재발급하는 서비스다.
 */
@Service
@RequiredArgsConstructor
public class RefreshService implements RefreshUseCase {
    private final LoadUserPort loadUserPort;
    private final RefreshTokenPort refreshTokenPort;
    private final RefreshTokenParser refreshTokenParser;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final TokenHasher tokenHasher;
    private final RefreshTokenProperties refreshTokenProperties;
    private final TokenIssuer tokenIssuer;

    @Override
    @Transactional
    public RefreshResult refresh(RefreshCommand command) {
        Instant now = Instant.now();

        // 쿠키로 전달된 refresh token 형식을 검증하고 저장용 해시를 만든다.
        String raw = refreshTokenParser.validate(command.refreshToken());
        String tokenHash = tokenHasher.sha256Hex(raw);

        // 아직 만료되지 않았고 revoke되지 않은 활성 refresh token만 재발급 대상으로 인정한다.
        var storedActive = refreshTokenPort.findActiveByTokenHash(tokenHash, now);
        if (storedActive.isEmpty()) {
            var storedAny = refreshTokenPort.findByTokenHash(tokenHash);
            if (storedAny.isEmpty()) {
                throw new BusinessException(AuthErrorCode.AUTH_REFRESH_INVALID);
            }

            var t = storedAny.get();
            if (!t.expiresAt().isAfter(now)) {
                throw new BusinessException(AuthErrorCode.AUTH_REFRESH_EXPIRED);
            }

            if (Boolean.TRUE.equals(t.revoked())) {
                throw new BusinessException(AuthErrorCode.AUTH_REFRESH_REVOKED);
            }

            throw new BusinessException(AuthErrorCode.AUTH_REFRESH_INVALID);
        }

        var stored = storedActive.get();
        var user = loadUserPort.findByUserId(stored.userId())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_REFRESH_INVALID));

        if (Boolean.FALSE.equals(user.enabled())) {
            throw new BusinessException(AuthErrorCode.AUTH_DISABLED_USER);
        }

        // refresh token 재사용을 막기 위해 새 토큰을 만들고 기존 토큰은 revoke한다.
        var generatedNew = refreshTokenGenerator.generate();
        String newRaw = generatedNew.rawToken();
        String newHash = tokenHasher.sha256Hex(newRaw);

        Instant newExpiresAt = now.plusSeconds(refreshTokenProperties.ttlSeconds());

        refreshTokenPort.revoke(stored.tokenHash());

        refreshTokenPort.save(new RefreshTokenPort.NewRefreshToken(
                newHash,
                stored.userId(),
                newExpiresAt
        ));

        // 재발급 시점의 실제 권한을 다시 조회해 access token claim에 반영한다.
        String newAccess = tokenIssuer.issueAccessToken(stored.userId(), user.roles());

        return new RefreshResult(newAccess, newRaw);
    }
}
