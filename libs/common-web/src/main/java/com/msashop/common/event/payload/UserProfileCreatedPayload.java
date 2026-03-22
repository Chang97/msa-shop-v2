package com.msashop.common.event.payload;

/**
 * user-service가 프로필 생성 성공 후 auth-service에 알려주는 결과 이벤트.
 * auth-service는 authUserId만 있으면 enabled=true 처리가 가능하다.
 */
public record UserProfileCreatedPayload(
        Long authUserId,
        Long userId
) {
}
