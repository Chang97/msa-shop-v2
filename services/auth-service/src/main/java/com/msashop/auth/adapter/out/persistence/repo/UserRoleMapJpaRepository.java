package com.msashop.auth.adapter.out.persistence.repo;

import com.msashop.auth.adapter.out.persistence.entity.UserRoleMapJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleMapJpaRepository extends JpaRepository<UserRoleMapJpaEntity, UserRoleMapJpaEntity.UserRoleMapId> {
    boolean existsByUserIdAndRoleId(Long userId, Integer roleId);
}
