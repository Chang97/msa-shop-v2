package com.msashop.auth.application.port.in;

import com.msashop.auth.adapter.in.web.dto.AuthMeResponse;

public interface GetAuthMeUseCase {
    AuthMeResponse getMe(Long authUserId);
}
