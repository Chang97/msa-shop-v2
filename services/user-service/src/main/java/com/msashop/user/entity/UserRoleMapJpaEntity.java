package com.msashop.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "user_role_map")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(UserRoleMapJpaEntity.UserRoleMapId.class)
public class UserRoleMapJpaEntity extends BaseAuditEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleJpaEntity role;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class UserRoleMapId implements Serializable {
        private Long user;
        private Integer role;
    }
}
