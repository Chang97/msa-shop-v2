package com.msashop.auth.application.port.out;

public interface CredentialPort {
    boolean existsByEmail(String email);
    boolean existsByLoginId(String loginId);

    /**
     * credential 저장 후 생성된 authUserId 반환.
     */
    Long saveCredential(String email, String loginId, String passwordHash);

    // 비활성화 처리
    void disable(Long authUserId);
}
