package com.msashop.user.query.application.port.in.model;

import java.time.Instant;
import java.util.List;

/**
 * Application 레이어의 결과 모델.
 * - Controller는 이 결과를 받아 Web DTO로 매핑만 수행한다.
 * - Query에 도메인 규칙이 없다면 domain.model 없이 Result로 충분하다.
 */
public record UserResult(
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