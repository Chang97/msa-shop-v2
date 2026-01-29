package com.msashop.user.application.port.in;

import com.msashop.user.application.port.in.model.UserResult;

public interface GetMeUseCase {
    public UserResult getMe(Long userId);
}
