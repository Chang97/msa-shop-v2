package com.msashop.auth.command.adapter.out.persistence.adapter;

import com.msashop.auth.command.adapter.out.persistence.mapper.RefreshTokenEntityMapper;
import com.msashop.auth.command.adapter.out.persistence.repo.RefreshTokenJpaRepository;
import com.msashop.auth.command.application.port.out.RefreshTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshTokenPersistenceAdapter implements RefreshTokenPort {

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;

    @Override
    public Optional<RefreshTokenRecord> findActiveByTokenId(String tokenId, Instant now) {
        return refreshTokenJpaRepository.findActiveByTokenId(tokenId, now)
                .map(RefreshTokenEntityMapper::toRecord);
    }

    @Override
    public Optional<RefreshTokenRecord> findByTokenId(String tokenId) {
        return refreshTokenJpaRepository.findByTokenId(tokenId)
                .map(RefreshTokenEntityMapper::toRecord);
    }

    @Override
    @Transactional
    public RefreshTokenRecord save(NewRefreshToken newToken) {
        var entity = RefreshTokenEntityMapper.toEntity(newToken);
        var saved = refreshTokenJpaRepository.save(entity);
        return RefreshTokenEntityMapper.toRecord(saved);
    }

    @Override
    @Transactional
    public void revoke(String tokenId, Instant revokedAt, Long revokedBy, String replacedByTokenId) {
        var entity = refreshTokenJpaRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found: " + tokenId));
        // revoked=true이면 revoked_at은 반드시 있어야 함
        entity.revoke(revokedAt, replacedByTokenId);
    }

    @Override
    @Transactional
    public void markUsed(String tokenId, Instant lastUsedAt) {
        var entity = refreshTokenJpaRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found: " + tokenId));
        entity.markUsed(lastUsedAt);
    }
}
