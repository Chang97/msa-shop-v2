package com.msashop.user.application.port.in.model;

import java.time.Instant;

/**
 * Application 계층의 결과 모델.
 */
public record UserResult(
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

