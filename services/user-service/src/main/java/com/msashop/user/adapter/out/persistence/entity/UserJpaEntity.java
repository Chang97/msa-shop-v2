package com.msashop.user.adapter.out.persistence.entity;

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

    @Column(name = "auth_user_id", nullable = false, unique = true)
    private Long authUserId;

    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(name = "emp_no", length = 100)
    private String empNo;

    @Column(name = "pstn_name", length = 200)
    private String pstnName;

    @Column(name = "tel", length = 100)
    private String tel;

    @Builder.Default
    @Column(name = "use_yn", nullable = false)
    private Boolean useYn = true;

    public void changeUseYn(boolean useYn) {
        this.useYn = useYn;
    }

    public void changeUserName(String userName) {
        this.userName = userName;
    }

    public void changeEmpNo(String empNo) {
        this.empNo = empNo;
    }

    public void changePstnName(String pstnName) {
        this.pstnName = pstnName;
    }

    public void changeTel(String tel) {
        this.tel = tel;
    }

    public void deactivate() {
        this.useYn = false;
    }
}

