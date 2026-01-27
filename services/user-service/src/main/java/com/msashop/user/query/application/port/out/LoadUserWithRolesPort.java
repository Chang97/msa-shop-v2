package com.msashop.user.query.application.port.out;

import com.msashop.user.query.application.port.out.model.UserWithRoleRow;

import java.util.List;

public interface LoadUserWithRolesPort {
    List<UserWithRoleRow> findMeWithRoles(Long userId);
}
