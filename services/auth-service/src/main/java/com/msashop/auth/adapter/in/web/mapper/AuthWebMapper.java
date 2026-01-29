package com.msashop.auth.adapter.in.web.mapper;

import com.msashop.auth.adapter.in.web.dto.LoginRequest;
import com.msashop.auth.adapter.in.web.dto.RegisterRequest;
import com.msashop.auth.application.port.in.model.LoginCommand;
import com.msashop.auth.application.port.in.model.RegisterCommand;

public final class AuthWebMapper {
    private AuthWebMapper() {}

    public static LoginCommand toCommand(LoginRequest req) {
        return new LoginCommand(req.loginId(), req.password());
    }

    public static RegisterCommand toRegisterCommand(RegisterRequest req) {
        return new RegisterCommand(
                req.email(),
                req.loginId(),
                req.password(),
                normalize(req.userName()),
                normalize(req.empNo()),
                normalize(req.pstnName()),
                normalize(req.tel())
        );
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

