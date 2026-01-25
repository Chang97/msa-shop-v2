package com.msashop.auth.command.application.port.out;

import java.util.Optional;

public interface LoadUserPort {
    Optional<AuthUserRecord> findByLoginId(String loginId);
    /**
     * application 레이어에서 필요한 만큼만 들고 오는 "조회 전용 레코드"
     * (도메인 모델을 아직 확정 안 했으면 이 방식이 가장 단순)
     */
    record AuthUserRecord(Long userId, String email, String loginId, String passwordHash, Boolean useYn) { }

}
