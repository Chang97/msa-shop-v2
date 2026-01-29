package com.msashop.auth.application.port.in;

import com.msashop.auth.application.port.in.model.LogoutCommand;

public interface LogoutUseCase {
    void logout(LogoutCommand command);
}

