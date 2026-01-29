package com.msashop.auth.application.port.in;

import com.msashop.auth.application.port.in.model.RegisterCommand;

/**
 * 회원가입 유스케이스.
 */
public interface RegisterUseCase {
    Long register(RegisterCommand command);
}
