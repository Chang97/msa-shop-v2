package com.msashop.auth.adapter.out.client.dto;

import java.time.Instant;

/**
 * user-service 프로필 생성 요청.
 */
public record ProvisionUserProfileRequest(
        Long authUserId,
        String userName,
        String empNo,
        String pstnName,
        String tel
) {}

