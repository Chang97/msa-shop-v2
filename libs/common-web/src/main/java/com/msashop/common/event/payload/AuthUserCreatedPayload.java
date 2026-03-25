package com.msashop.common.event.payload;

/**
 * auth-service가 회원가입 직후 user-service에 넘기는 최소 정보.
 * user-service는 이 payload만으로 프로필 row를 만들 수 있어야 한다.
 */
public record AuthUserCreatedPayload(
        Long authUserId,
        String userName,
        String empNo,
        String pstnName,
        String tel
) {
}
