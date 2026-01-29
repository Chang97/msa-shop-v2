package com.msashop.auth.adapter.out.persistence.repo;

import com.msashop.auth.adapter.out.persistence.entity.RefreshTokenEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Query("""
        select rt
        from RefreshTokenEntity rt
        where rt.tokenHash = :tokenHash
        and rt.revoked = false
        and rt.expiresAt > :now
    """)
    Optional<RefreshTokenEntity> findActiveByTokenHash(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    long countByUserId(Long userId);

    @Profile("test")
    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE auth_refresh_token RESTART IDENTITY CASCADE", nativeQuery = true)
    void truncateAll();
}

