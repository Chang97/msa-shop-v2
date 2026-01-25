package com.msashop.auth.command.adapter.out.persistence.adapter;

import com.msashop.auth.command.adapter.out.persistence.mapper.RefreshTokenEntityMapper;
import com.msashop.auth.command.adapter.out.persistence.repo.RefreshTokenJpaRepository;
import com.msashop.auth.command.application.port.out.RefreshTokenPort;
import com.msashop.auth.common.exception.ErrorCode;
import com.msashop.auth.common.exception.NotFoundException;
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
//        return refreshTokenJpaRepository.findByTokenIdAndRevokedFalseAndExpiresAtAfter(tokenId, now)
                .map(RefreshTokenEntityMapper::toRecord);
    }

    @Override
    public Optional<RefreshTokenRecord> findByTokenId(String tokenId) {
        return refreshTokenJpaRepository.findByTokenId(tokenId)
                .map(RefreshTokenEntityMapper::toRecord);
    }

    @Override
    @Transactional
    public void save(NewRefreshToken newToken) {
        var entity = RefreshTokenEntityMapper.toEntity(newToken);
        var saved = refreshTokenJpaRepository.save(entity);
        RefreshTokenEntityMapper.toRecord(saved);
    }

    @Override
    @Transactional
    public void revoke(String tokenId, Instant revokedAt, String replacedByTokenId) {
        var entity = refreshTokenJpaRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.COMMON_NOT_FOUND, "Refresh token not found: " + tokenId));
        // revoked=true이면 revoked_at은 반드시 있어야 함
        entity.revoke(revokedAt, replacedByTokenId);
    }

    @Override
    @Transactional
    public void markUsed(String tokenId, Instant lastUsedAt) {
        var entity = refreshTokenJpaRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.COMMON_NOT_FOUND, "Refresh token not found: " + tokenId));
        entity.markUsed(lastUsedAt);
    }
}
