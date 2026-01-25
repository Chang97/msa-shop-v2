package com.msashop.auth.command.application.port.in;

import com.msashop.auth.command.application.port.in.model.LoginCommand;
import com.msashop.auth.command.application.port.in.model.LoginResult;

public interface LoginUseCase {
    /* 토큰 응답 타입은 일단 String/record로 */
    LoginResult login(LoginCommand command);
}
