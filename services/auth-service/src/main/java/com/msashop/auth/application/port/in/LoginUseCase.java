package com.msashop.auth.application.port.in;

import com.msashop.auth.application.port.in.model.LoginCommand;
import com.msashop.auth.application.port.in.model.LoginResult;

public interface LoginUseCase {
    LoginResult login(LoginCommand command);
}

