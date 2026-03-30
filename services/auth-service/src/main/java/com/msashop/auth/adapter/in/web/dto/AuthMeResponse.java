package com.msashop.auth.adapter.in.web.dto;

import java.util.List;

public record AuthMeResponse(
        Long authUserId,
        String email,
        String loginId,
        boolean enabled,
        List<String> roles
) {
}
