package com.msashop.auth.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * user-role 매핑(entity).
 */
@Entity
@Table(name = "user_role_map")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(UserRoleMapJpaEntity.UserRoleMapId.class)
public class UserRoleMapJpaEntity extends BaseAuditEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "role_id", nullable = false)
    private Integer roleId;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class UserRoleMapId implements Serializable {
        private Long userId;
        private Integer roleId;
    }
}

