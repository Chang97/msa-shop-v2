package com.msashop.user.query.application.port.in;

import com.msashop.user.query.application.port.in.model.UserResult;

public interface GetMeUseCase {
    public UserResult getMe(Long userId);
}
