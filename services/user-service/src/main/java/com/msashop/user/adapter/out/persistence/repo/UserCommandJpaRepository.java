package com.msashop.user.adapter.out.persistence.repo;

import com.msashop.user.adapter.out.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCommandJpaRepository extends JpaRepository<UserJpaEntity, Long> {
    Optional<UserJpaEntity> findByAuthUserId(Long authUserId);
    boolean existsByAuthUserId(Long authUserId);
}