package com.msashop.auth.adapter.out.persistence.repo;

import com.msashop.auth.adapter.out.persistence.entity.AuthUserCredentialJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthUserCredentialJpaRepository extends JpaRepository<AuthUserCredentialJpaEntity, Long> {
    boolean existsByEmail(String email);
    boolean existsByLoginId(String loginId);

    Optional<AuthUserCredentialJpaEntity> findByLoginId(String loginId);
}
