package com.msashop.auth.command.application.port.out;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenPort {
    Optional<RefreshTokenRecord> findActiveByTokenId(String tokenId, Instant now);

    Optional<RefreshTokenRecord> findByTokenId(String tokenId);

    void save(NewRefreshToken newToken);

    void revoke(String tokenId, Instant revokedAt, String replacedByTokenId);

    void markUsed(String tokenId, Instant lastUsedAt);

    record RefreshTokenRecord(
            Long refreshTokenId,
            String tokenId,
            String tokenHash,
            Long userId,
            Instant expiresAt,
            Instant lastUsedAt,
            Boolean revoked,
            Instant revokedAt,
            String replacedByTokenId
    ) {}

    record NewRefreshToken(
            String tokenId,
            String tokenHash,
            Long userId,
            Instant expiresAt
    ) {}
}
