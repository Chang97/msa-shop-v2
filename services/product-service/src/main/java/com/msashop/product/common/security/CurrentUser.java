package com.msashop.product.common.security;

import java.util.Set;

// =====================================================
// CurrentUser (Principal로 사용할 모델)
// - 컨트롤러/서비스에서 헤더를 직접 다루지 않게 만들기 위한 DTO
// - userId, roles만 담는다 (민감정보 X)
// =====================================================
public record CurrentUser(
        Long userId,
        Set<String> roles
) {
}
