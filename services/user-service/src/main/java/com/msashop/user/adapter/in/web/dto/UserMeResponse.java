package com.msashop.user.adapter.in.web.dto;

import java.time.Instant;

/**
 * GET /api/users/me 응답 DTO.
 */
public record UserMeResponse(
        Long userId,
        Long authUserId,
        String userName,
        String empNo,
        String pstnName,
        String tel,
        boolean useYn,
        Instant createdAt,
        Long createdBy,
        Instant updatedAt,
        Long updatedBy
) {}

