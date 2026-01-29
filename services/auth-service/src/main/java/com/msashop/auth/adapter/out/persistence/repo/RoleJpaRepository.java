package com.msashop.auth.adapter.out.persistence.repo;

import com.msashop.auth.adapter.out.persistence.entity.RoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, Integer> {
    Optional<RoleJpaEntity> findByRoleName(String roleName);
}

