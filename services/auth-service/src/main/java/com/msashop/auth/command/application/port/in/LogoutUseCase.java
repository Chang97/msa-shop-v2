package com.msashop.auth.command.application.port.in;

import com.msashop.auth.command.application.port.in.model.LogoutCommand;

public interface LogoutUseCase {
    void logout(LogoutCommand command);
}
