package com.msashop.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * users 테이블 (DDL 기준) 매핑 엔티티.
 * - Query에서 projection으로 뽑더라도, JPQL 안정성을 위해 엔티티 필드/컬럼명을 정확히 맞춘다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserJpaEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(name = "login_id", length = 100)
    private String loginId;

    @Column(name = "user_password", length = 400)
    private String userPassword;

    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(name = "emp_no", length = 100)
    private String empNo;

    @Column(name = "pstn_name", length = 200)
    private String pstnName;

    @Column(name = "tel", length = 100)
    private String tel;

    @Column(name = "user_password_update_dt")
    private Instant userPasswordUpd;

    @Column(name = "user_password_fail_cnt")
    private Integer userPasswordFailCnt;

    @Column(name = "old1_user_password", length = 400)
    private String old1UserPassword;

    @Column(name = "use_yn", nullable = false)
    private Boolean useYn;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UserRoleMapJpaEntity> userRoleMaps = new HashSet<>();

    public void deactivate() {
        this.useYn = false;
    }

}
