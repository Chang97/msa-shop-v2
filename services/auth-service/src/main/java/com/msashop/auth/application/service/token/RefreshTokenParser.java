package com.msashop.auth.application.service.token;

import com.msashop.common.web.exception.AuthErrorCode;
import com.msashop.common.web.exception.UnauthorizedException;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenParser {

    public String validate(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException(AuthErrorCode.AUTH_REFRESH_MISSING);
        }
        return rawRefreshToken;
    }
}

