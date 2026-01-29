package com.msashop.auth.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "role_name", nullable = false, length = 200, unique = true)
    private String roleName;

    @Builder.Default
    @Column(name = "use_yn", nullable = false)
    private Boolean useYn = true;
}

