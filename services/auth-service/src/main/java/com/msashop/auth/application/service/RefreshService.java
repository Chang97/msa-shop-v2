package com.msashop.auth.application.service;

import com.msashop.auth.application.port.in.RefreshUseCase;
import com.msashop.auth.application.port.in.model.RefreshCommand;
import com.msashop.auth.application.port.in.model.RefreshResult;
import com.msashop.auth.application.port.out.RefreshTokenPort;
import com.msashop.auth.application.service.token.RefreshTokenGenerator;
import com.msashop.auth.application.service.token.RefreshTokenParser;
import com.msashop.auth.application.service.token.TokenHasher;
import com.msashop.auth.application.service.token.TokenIssuer;
import com.msashop.auth.config.auth.RefreshTokenProperties;
import com.msashop.common.web.exception.AuthErrorCode;
import com.msashop.common.web.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefreshService implements RefreshUseCase {
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

        String raw = refreshTokenParser.validate(command.refreshToken());
        String tokenHash = tokenHasher.sha256Hex(raw);

        var storedActive = refreshTokenPort.findActiveByTokenHash(tokenHash, now);
        if (storedActive.isEmpty()) {
            var storedAny = refreshTokenPort.findByTokenHash(tokenHash);
            if (storedAny.isEmpty()) {
                throw new UnauthorizedException(AuthErrorCode.AUTH_INVALID_CREDENTIALS);
            }

            var t = storedAny.get();
            if (!t.expiresAt().isAfter(now)) {
                throw new UnauthorizedException(AuthErrorCode.AUTH_REFRESH_EXPIRED);
            }

            if (Boolean.TRUE.equals(t.revoked())) {
                throw new UnauthorizedException(AuthErrorCode.AUTH_REFRESH_REVOKED);
            }

            throw new UnauthorizedException(AuthErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        var stored = storedActive.get();

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

        String newAccess = tokenIssuer.issueAccessToken(stored.userId(), List.of("ROLE_USER"));

        return new RefreshResult(newAccess, newRaw);
    }
}
