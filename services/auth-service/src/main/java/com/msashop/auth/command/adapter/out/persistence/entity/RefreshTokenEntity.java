package com.msashop.auth.command.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenEntity extends BaseAuditEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_id")
    private Long refreshTokenId;

    @Column(name = "token_id", length = 64, nullable = false, unique = true)
    private String tokenId;

    @Column(name = "token_hash", length = 512, nullable = false)
    private String tokenHash;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Builder.Default
    @Column(nullable = false)
    private Boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_token_id", length = 64)
    private String replacedByTokenId;

    public void markUsed(Instant now) {
        this.lastUsedAt = now;
    }
    // DDL에 check constraint가 있어서 revoke 처리 시 반드시 revokedAt을 채워야 함.
    public void revoke(Instant now, String replacedByTokenId) {
        this.revoked = true;
        this.revokedAt = now;
        this.replacedByTokenId = replacedByTokenId;
    }
}
