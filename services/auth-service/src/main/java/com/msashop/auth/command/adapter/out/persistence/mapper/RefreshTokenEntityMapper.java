package com.msashop.auth.command.adapter.out.persistence.mapper;

import com.msashop.auth.command.adapter.out.persistence.entity.RefreshTokenEntity;
import com.msashop.auth.command.application.port.out.RefreshTokenPort;

public final class RefreshTokenEntityMapper {
    private RefreshTokenEntityMapper() {}

    public static RefreshTokenPort.RefreshTokenRecord toRecord(RefreshTokenEntity e) {
        return new RefreshTokenPort.RefreshTokenRecord(
                e.getRefreshTokenId(),
                e.getTokenId(),
                e.getTokenHash(),
                e.getUserId(),
                e.getExpiresAt(),
                e.getLastUsedAt(),
                e.getRevoked(),
                e.getRevokedAt(),
                e.getReplacedByTokenId()
        );
    }

    public static RefreshTokenEntity toEntity(RefreshTokenPort.NewRefreshToken n) {
        return RefreshTokenEntity.builder()
                .tokenId(n.tokenId())
                .tokenHash(n.tokenHash())
                .userId(n.userId())
                .expiresAt(n.expiresAt())
                .build();
    }
}
