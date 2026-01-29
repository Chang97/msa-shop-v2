package com.msashop.auth.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * auth DB의 credential 테이블(예시).
 * - 실제 컬럼명/테이블명은 네 DDL에 맞춰 조정.
 */
@Entity
@Table(name = "auth_user_credential")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUserCredentialJpaEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 200, unique = true)
    private String email;

    @Column(name = "login_id", nullable = false, length = 100, unique = true)
    private String loginId;

    @Column(name = "password_hash", nullable = false, length = 400)
    private String passwordHash;

    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    public void disable() {
        enabled = false;
    }
}
