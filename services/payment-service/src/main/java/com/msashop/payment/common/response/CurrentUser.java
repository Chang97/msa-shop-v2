package com.msashop.payment.common.response;

import java.util.Set;

public record CurrentUser(Long userId, Set<String> roles) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}

