package com.msashop.auth.command.adapter.in.web.mapper;

import com.msashop.auth.command.adapter.in.web.dto.LoginRequest;
import com.msashop.auth.command.application.port.in.model.LoginCommand;

public final class AuthWebMapper {
    private AuthWebMapper() {}

    public static LoginCommand toCommand(LoginRequest req) {
        return new LoginCommand(req.loginId(), req.password());
    }
}
