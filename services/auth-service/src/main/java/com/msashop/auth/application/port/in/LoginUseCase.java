package com.msashop.auth.application.port.in;

import com.msashop.auth.application.port.in.model.LoginCommand;
import com.msashop.auth.application.port.in.model.LoginResult;

public interface LoginUseCase {
    /* ?좏겙 ?묐떟 ??낆? ?쇰떒 String/record濡?*/
    LoginResult login(LoginCommand command);
}

