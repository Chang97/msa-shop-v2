package com.msashop.user.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * auth-service 신규 사용자 프로필 생성 요청.
 */
public record ProvisionUserProfileRequest(
        @NotNull Long authUserId,
        @Size(max = 100) String userName,
        @Size(max = 100) String empNo,
        @Size(max = 200) String pstnName,
        @Size(max = 100) String tel
) {}
