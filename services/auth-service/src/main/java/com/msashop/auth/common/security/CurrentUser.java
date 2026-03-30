package com.msashop.auth.common.security;

import java.util.Collections;
import java.util.Set;

/**
 * Gateway가 전달한 사용자 헤더를 서비스 내부 principal 형태로 담는 모델이다.
 */
public record CurrentUser(Long userId, Set<String> roles) {

    public Set<String> safeRoles() {
        return roles == null ? Collections.emptySet() : roles;
    }
}
