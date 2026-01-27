package com.msashop.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "role")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleJpaEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Integer roleId;

    @Column(name = "role_name", nullable = false, length = 200)
    private String roleName;

    @Column(name = "use_yn", nullable = false)
    private Boolean useYn;

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UserRoleMapJpaEntity> userRoleMaps = new HashSet<>();
}
