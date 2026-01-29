package com.msashop.auth.adapter.in.web.dto;

import java.time.Instant;

/**
 * 회원가입 응답 DTO(최소).
 * - 필요하면 accessToken을 같이 내려도 되지만, 보통은 별도 login으로 분리하는 편이 깔끔하다.
 */
public record RegisterResponse(
        Long userId,
        Instant registeredAt
) {
}
