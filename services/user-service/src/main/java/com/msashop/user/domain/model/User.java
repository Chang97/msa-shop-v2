package com.msashop.user.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * User Aggregate (Command-side).
 * - 프레임워크 의존성 없이 순수하게 상태/규칙만 가진다.
 */
public class User {

    private final Long userId;

    // profile
    private String userName;
    private String empNo;
    private String pstnName;
    private String tel;

    // status
    private boolean useYn;

    // audit (정말 필요할 때만 유지 권장)
    private Instant updatedAt;
    private Long updatedBy;

    /**
     * 최소 생성자.
     * - 조회/수정/비활성화의 기준이 되는 필드만 우선 받는다.
     * - 나머지 프로필 값들은 팩토리/매퍼에서 세팅하거나, 별도 생성자/정적 팩토리로 확장한다.
     */
    public User(Long userId, boolean useYn, Instant updatedAt, Long updatedBy) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.useYn = useYn;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    // ---------- getters (스타일 통일) ----------
    public Long getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getEmpNo() { return empNo; }
    public String getPstnName() { return pstnName; }
    public String getTel() { return tel; }
    public boolean isUseYn() { return useYn; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getUpdatedBy() { return updatedBy; }

    // ---------- behaviors ----------
    /**
     * 계정 비활성화 (멱등).
     */
    public void deactivate() {
        this.useYn = false;
    }

    /**
     * 프로필 부분 수정.
     * - null이면 변경하지 않는다.
     */
    public void updateProfile(String userName, String empNo, String pstnName, String tel) {
        if (userName != null) this.userName = userName;
        if (empNo != null) this.empNo = empNo;
        if (pstnName != null) this.pstnName = pstnName;
        if (tel != null) this.tel = tel;
    }
}
