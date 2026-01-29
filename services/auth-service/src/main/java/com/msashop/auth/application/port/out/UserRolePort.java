package com.msashop.auth.application.port.out;

/**
 * user-service에 프로필 생성 요청을 보내는 포트.
 * - 나중에 이벤트 기반으로 바꿔도 application은 변경 최소.
 */
public interface UserRolePort {
    void assignRole(Long userId, String roleName);
}
