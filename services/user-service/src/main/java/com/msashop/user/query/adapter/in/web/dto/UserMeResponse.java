package com.msashop.user.query.adapter.in.web.dto;

import java.time.Instant;
import java.util.List;

/**
 * GET /api/users/me 응답 DTO.
 *
 * - roles는 List<String>으로 내려준다.
 * - 감사 컬럼은 BaseAuditEntity 기반으로 내려주는 정책을 반영한다.
 */
public record UserMeResponse(
        Long userId,
        String email,
        String loginId,
        String userName,
        String empNo,
        String pstnName,
        String tel,
        boolean useYn,
        List<String> roles,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy
) {}
