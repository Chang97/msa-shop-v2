package com.msashop.user.command.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Command-side User Aggregate.
 * - 프레임워크 의존성 없이 순수하게 상태/규칙만 가진다.
 */
public class User {

    private final Long userId;
    private boolean useYn;

    // audit(필요 시 도메인에서 보관할지 여부는 선택)
    private Instant updatedAt;
    private Long updatedBy;

    public User(Long userId, boolean useYn, Instant updatedAt, Long updatedBy) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.useYn = useYn;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public Long userId() { return userId; }
    public boolean useYn() { return useYn; }

    /**
     * 계정 비활성화.
     * - 이미 비활성인 경우 정책에 따라:
     *   1) 멱등 처리(그냥 return)
     *   2) 예외 처리(Conflict)
     *
     * 여기서는 "멱등"으로 둔다(운영 안정성).
     */
    public void deactivate() {
        this.useYn = false;
    }
}
