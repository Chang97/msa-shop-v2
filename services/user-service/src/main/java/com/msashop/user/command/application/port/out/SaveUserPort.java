package com.msashop.user.command.application.port.out;

import com.msashop.user.command.domain.model.User;

/**
 * User aggregate 저장 (command 모델 기준).
 */
public interface SaveUserPort {
    void save(User user);
}
