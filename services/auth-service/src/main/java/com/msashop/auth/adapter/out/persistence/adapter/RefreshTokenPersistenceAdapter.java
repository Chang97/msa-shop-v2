package com.msashop.auth.adapter.out.persistence.adapter;

import com.msashop.auth.adapter.out.persistence.mapper.RefreshTokenEntityMapper;
import com.msashop.auth.adapter.out.persistence.repo.RefreshTokenJpaRepository;
import com.msashop.auth.application.port.out.RefreshTokenPort;
import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.CommonErrorCode;
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
    public Optional<RefreshTokenRecord> findActiveByTokenHash(String tokenHash, Instant now) {
        return refreshTokenJpaRepository.findActiveByTokenHash(tokenHash, now)
                .map(RefreshTokenEntityMapper::toRecord);
    }

    @Override
    public Optional<RefreshTokenRecord> findByTokenHash(String tokenHash) {
        return refreshTokenJpaRepository.findByTokenHash(tokenHash)
                .map(RefreshTokenEntityMapper::toRecord);
    }

    @Override
    @Transactional
    public void save(NewRefreshToken newToken) {
        var entity = RefreshTokenEntityMapper.toEntity(newToken);
        refreshTokenJpaRepository.save(entity);
    }

    @Override
    @Transactional
    public void revoke(String tokenHash) {
        var entity = refreshTokenJpaRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.COMMON_NOT_FOUND, "리프레시 토큰을 찾을 수 없습니다: " + tokenHash));
        entity.revoke();
    }
}
