package com.msashop.auth.application.port.out;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenPort {
    Optional<RefreshTokenRecord> findActiveByTokenHash(String tokenHash, Instant now);

    Optional<RefreshTokenRecord> findByTokenHash(String tokenHash);

    void save(NewRefreshToken newToken);

    void revoke(String tokenHash);

    record RefreshTokenRecord(
            Long refreshTokenId,
            String tokenHash,
            Long userId,
            Instant expiresAt,
            Boolean revoked
    ) {}

    record NewRefreshToken(
            String tokenHash,
            Long userId,
            Instant expiresAt
    ) {}
}

