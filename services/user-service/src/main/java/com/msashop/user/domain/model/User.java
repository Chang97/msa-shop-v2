package com.msashop.user.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * 사용자 프로필 command/query 처리를 위한 도메인 aggregate.
 */
public class User {

    private final Long userId;
    private final Long authUserId;

    // profile
    private String userName;
    private String empNo;
    private String pstnName;
    private String tel;

    // status
    private boolean useYn;

    // audit
    private final Instant createdAt;
    private final Long createdBy;
    private Instant updatedAt;
    private Long updatedBy;

    /**
     * 사용자 프로필 조회/수정/비활성화에 필요한 상태로 aggregate를 복원한다.
     */
    public User(
            Long userId,
            Long authUserId,
            String userName,
            String empNo,
            String pstnName,
            String tel,
            boolean useYn,
            Instant createdAt,
            Long createdBy,
            Instant updatedAt,
            Long updatedBy
    ) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.authUserId = authUserId;
        this.userName = userName;
        this.empNo = empNo;
        this.pstnName = pstnName;
        this.tel = tel;
        this.useYn = useYn;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public Long getUserId() { return userId; }
    public Long getAuthUserId() { return authUserId; }
    public String getUserName() { return userName; }
    public String getEmpNo() { return empNo; }
    public String getPstnName() { return pstnName; }
    public String getTel() { return tel; }
    public boolean isUseYn() { return useYn; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getUpdatedBy() { return updatedBy; }

    /**
     * 사용자 프로필을 비활성화한다. 이미 비활성화된 경우에도 동일 상태를 유지한다.
     */
    public void deactivate() {
        this.useYn = false;
    }

    /**
     * 전달된 폼 값으로 현재 프로필 값을 전체 반영한다.
     */
    public void updateProfile(String userName, String empNo, String pstnName, String tel) {
        this.userName = userName;
        this.empNo = empNo;
        this.pstnName = pstnName;
        this.tel = tel;
    }
}
