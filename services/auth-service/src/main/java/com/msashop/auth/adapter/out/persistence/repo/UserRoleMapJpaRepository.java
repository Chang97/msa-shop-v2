package com.msashop.auth.adapter.out.persistence.repo;

import com.msashop.auth.adapter.out.persistence.entity.UserRoleMapJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRoleMapJpaRepository extends JpaRepository<UserRoleMapJpaEntity, UserRoleMapJpaEntity.UserRoleMapId> {
    boolean existsByUserIdAndRoleId(Long userId, Integer roleId);

    @Query("select r.roleName from UserRoleMapJpaEntity m join RoleJpaEntity r on m.roleId = r.roleId where m.userId = :userId and r.useYn = true")
    List<String> findRoleNamesByUserId(@Param("userId") Long userId);
}
