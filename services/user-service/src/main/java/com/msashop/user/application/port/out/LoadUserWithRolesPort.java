package com.msashop.user.application.port.out;

import com.msashop.user.application.port.out.model.UserRow;

import java.util.List;
import java.util.Optional;

public interface LoadUserWithRolesPort {
    Optional<UserRow> findByAuthUserId(Long userId);
}
