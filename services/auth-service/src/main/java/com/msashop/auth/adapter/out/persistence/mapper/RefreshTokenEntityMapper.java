package com.msashop.auth.adapter.out.persistence.mapper;

import com.msashop.auth.adapter.out.persistence.entity.RefreshTokenEntity;
import com.msashop.auth.application.port.out.RefreshTokenPort;

public final class RefreshTokenEntityMapper {
    private RefreshTokenEntityMapper() {}

    public static RefreshTokenPort.RefreshTokenRecord toRecord(RefreshTokenEntity e) {
        return new RefreshTokenPort.RefreshTokenRecord(
                e.getRefreshTokenId(),
                e.getTokenHash(),
                e.getUserId(),
                e.getExpiresAt(),
                e.getRevoked()
        );
    }

    public static RefreshTokenEntity toEntity(RefreshTokenPort.NewRefreshToken n) {
        return RefreshTokenEntity.builder()
                .tokenHash(n.tokenHash())
                .userId(n.userId())
                .expiresAt(n.expiresAt())
                .build();
    }
}

