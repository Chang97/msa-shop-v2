package com.msashop.user.adapter.in.web.dto;

import jakarta.validation.constraints.Size;

/**
 * PATCH /api/users/me 요청 DTO.
 * - null이면 "미수정" 의미
 */
public record UserMeUpdateRequest(
        @Size(max = 100) String userName,
        @Size(max = 100) String empNo,
        @Size(max = 200) String pstnName,
        @Size(max = 100) String tel
) {
}
