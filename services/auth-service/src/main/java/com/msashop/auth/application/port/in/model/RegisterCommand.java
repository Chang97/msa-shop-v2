package com.msashop.auth.application.port.in.model;

/**
 * application layer command.
 * - web dto를 application이 직접 참조하지 않기 위함.
 */
public record RegisterCommand(
        String email,
        String loginId,
        String rawPassword,
        String userName,
        String empNo,
        String pstnName,
        String tel
) {
}
