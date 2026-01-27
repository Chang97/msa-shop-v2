package com.msashop.user.query.adapter.out.persistence.repo;

import com.msashop.user.entity.UserJpaEntity;
import com.msashop.user.query.application.port.out.model.UserWithRoleRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Query 전용 Repository.
 *
 * 핵심:
 * - user 1명에 roles가 N개면 조인 결과 row가 N개가 된다.
 * - 따라서 Projection Row를 List로 받고,
 *   application service에서 "user 1 + roles list"로 집계한다.
 */
public interface UserQueryJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    /**
     * 엔티티 관계 기반 JPQL 조인.
     *
     * 전제(예시):
     * - UserJpaEntity.userRoleMaps : Set<UserRoleMapJpaEntity>
     * - UserRoleMapJpaEntity.role : RoleJpaEntity
     * - RoleJpaEntity.roleName : String
     *
     * 만약 네 필드명이 다르면 아래 path만 너 엔티티에 맞게 바꾸면 된다.
     */
    @Query("""
           select
             u.userId as userId,
             u.email as email,
             u.loginId as loginId,
             u.userName as userName,
             u.empNo as empNo,
             u.pstnName as pstnName,
             u.tel as tel,
             u.useYn as useYn,

             u.createdAt as createdAt,
             u.createdBy as createdBy,
             u.updatedAt as updatedAt,
           u.updatedBy as updatedBy,

           r.roleName as roleName
           from UserJpaEntity u
           left join u.userRoleMaps urm
           left join urm.role r
           where u.userId = :userId
           """)
    List<UserWithRoleRow> findUserWithRolesRows(@Param("userId") Long userId);
}
