package com.msashop.user.command.application.port.in;

/**
 * 내 계정 비활성화 유스케이스.
 */
public interface DeactivateMeUseCase {
    void deactivateMe(Long userId);
}
