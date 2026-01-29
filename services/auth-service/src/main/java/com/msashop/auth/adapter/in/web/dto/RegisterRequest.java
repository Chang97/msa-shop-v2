package com.msashop.auth.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 DTO.
 * - validation은 web layer에서 1차로 걸러준다.
 */
public record RegisterRequest(
        @Email @NotBlank @Size(max = 200) String email,
        @NotBlank @Size(max = 100) String loginId,
        @NotBlank @Size(min = 8, max = 200) String password,
        @Size(max = 100) String userName,
        @Size(max = 100) String empNo,
        @Size(max = 200) String pstnName,
        @Size(max = 100) String tel
) {
}
