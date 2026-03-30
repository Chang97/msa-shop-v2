package com.msashop.auth.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 DTO다.
 * 형식 검증은 웹 계층에서 1차로 처리한다.
 */
public record RegisterRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max = 200, message = "이메일은 200자 이하여야 합니다.")
        String email,

        @NotBlank(message = "아이디는 필수입니다.")
        @Size(max = 100, message = "아이디는 100자 이하여야 합니다.")
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 4, max = 200, message = "비밀번호는 4자 이상 200자 이하여야 합니다.")
        String password,

        @Size(max = 100, message = "이름은 100자 이하여야 합니다.")
        String userName,

        @Size(max = 100, message = "사번은 100자 이하여야 합니다.")
        String empNo,

        @Size(max = 200, message = "소속명은 200자 이하여야 합니다.")
        String pstnName,

        @Size(max = 100, message = "전화번호는 100자 이하여야 합니다.")
        String tel
) {
}
