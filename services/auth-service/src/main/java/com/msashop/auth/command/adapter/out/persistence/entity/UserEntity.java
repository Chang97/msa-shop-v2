package com.msashop.auth.command.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity extends BaseAuditEntity {

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

}
