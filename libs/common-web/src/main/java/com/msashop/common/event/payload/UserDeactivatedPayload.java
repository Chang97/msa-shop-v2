package com.msashop.common.event.payload;

/**
 * user-service가 사용자 비활성화를 완료한 뒤 auth-service에 전달하는 이벤트 payload.
 */
public record UserDeactivatedPayload(
        Long authUserId,
        Long userId
) {
}
