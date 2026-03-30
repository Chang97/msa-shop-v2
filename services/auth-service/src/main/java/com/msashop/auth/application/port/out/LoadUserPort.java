package com.msashop.auth.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * 로그인에 필요한 사용자 인증 정보를 조회하는 아웃바운드 포트다.
 */
public interface LoadUserPort {
    Optional<AuthUserRecord> findByLoginId(String loginId);
    Optional<AuthUserRecord> findByUserId(Long userId);

    /**
     * application 계층에서 인증에 필요한 최소 사용자 정보를 담는 조회 전용 결과다.
     * JPA 엔티티를 그대로 노출하지 않고 로그인 검증에 필요한 값만 전달한다.
     */
    record AuthUserRecord(
            Long userId,
            String email,
            String loginId,
            String passwordHash,
            Boolean enabled,
            List<String> roles
    ) {
    }
}
