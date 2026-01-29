package com.msashop.user.application.port.out;

import com.msashop.user.domain.model.User;

import java.util.Optional;

/**
 * User aggregate 조회 (command 모델 기준).
 */
public interface LoadUserPort {
    Optional<User> findById(Long userId);

    Optional<User> findByAuthUserId(Long authUserId);
}
