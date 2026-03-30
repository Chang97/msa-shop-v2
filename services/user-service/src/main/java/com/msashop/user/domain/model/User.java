package com.msashop.user.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * User aggregate for command-side operations.
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
     * Rehydrates the aggregate with the minimum state needed for user profile commands and queries.
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
     * Deactivates the user profile. This operation is idempotent.
     */
    public void deactivate() {
        this.useYn = false;
    }

    /**
     * Updates only the provided profile fields and keeps existing values for null inputs.
     */
    public void updateProfile(String userName, String empNo, String pstnName, String tel) {
        if (userName != null) this.userName = userName;
        if (empNo != null) this.empNo = empNo;
        if (pstnName != null) this.pstnName = pstnName;
        if (tel != null) this.tel = tel;
    }
}
