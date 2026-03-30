package com.msashop.auth.application.service.token;

import com.msashop.common.web.exception.AuthErrorCode;
import com.msashop.common.web.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenParser {

    public String validate(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BusinessException(AuthErrorCode.AUTH_REFRESH_MISSING);
        }
        return rawRefreshToken;
    }
}
