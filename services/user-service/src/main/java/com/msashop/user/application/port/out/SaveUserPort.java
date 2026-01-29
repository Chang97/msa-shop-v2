package com.msashop.user.application.port.out;

import com.msashop.user.domain.model.User;

/**
 * User aggregate 저장 (command 모델 기준).
 */
public interface SaveUserPort {
    void save(User user);
    void deactivate(User user);
}
