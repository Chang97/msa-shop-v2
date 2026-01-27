package com.msashop.user.common.security;

import java.util.Collections;
import java.util.Set;

/**
 * 표준 Principal 모델.
 *
 * - Gateway가 JWT를 검증한 뒤, 내부 서비스로 전달한 헤더(X-User-Id, X-Roles)를
 *   서비스 내부 SecurityContext로 승격했을 때 사용한다.
 * - 서비스 컨트롤러에서 @AuthenticationPrincipal CurrentUser 로 바로 주입받는 용도.
 *
 * 주의:
 * - userId는 "서비스 내부 식별자" 기준으로 Long 사용.
 * - roles는 중복 제거 및 빠른 포함 검사 때문에 Set 사용.
 */
public record CurrentUser(Long userId, Set<String> roles) {

    /**
     * roles가 null로 들어오는 사고를 방지하기 위해 안전 접근 제공.
     */
    public Set<String> safeRoles() {
        return roles == null ? Collections.emptySet() : roles;
    }

    /**
     * 특정 권한 보유 여부를 빠르게 확인하기 위한 헬퍼.
     * (컨트롤러/서비스에서 보조적으로 쓸 수 있음)
     */
    public boolean hasRole(String role) {
        return safeRoles().contains(role);
    }
}
