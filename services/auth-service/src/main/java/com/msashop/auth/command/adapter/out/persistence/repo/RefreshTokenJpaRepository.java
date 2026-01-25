package com.msashop.auth.command.adapter.out.persistence.repo;

import com.msashop.auth.command.adapter.out.persistence.entity.RefreshTokenEntity;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {
    Optional<RefreshTokenEntity> findByTokenId(String tokenId);

    // 현재 유효한 refresh 토큰만 가져오기 (revoked=false AND expires_at > now)
    @Query("""
        select rt
        from RefreshTokenEntity rt
        where rt.tokenId = :tokenId
        and rt.revoked = false
        and rt.expiresAt > :now
    """)
    Optional<RefreshTokenEntity> findActiveByTokenId(@Param("tokenId") String tokenId, @Param("now") Instant now);

    @Query("""
        select rt
        from RefreshTokenEntity rt
        where rt.userId = :userId
        and rt.revoked = false
        and rt.expiresAt > :now
    """)
    Optional<RefreshTokenEntity> findActiveByUserId(@Param("userId") Long userId, @Param("now") Instant now);

}
