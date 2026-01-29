package com.msashop.user.application.port.in;

import com.msashop.user.application.port.in.model.UpdateMeCommand;

/**
 * 내 정보 수정 유스케이스.
 */
public interface UpdateMeUseCase {
    void updateMe(Long userId, UpdateMeCommand command);
}
