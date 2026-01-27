package com.msashop.auth.command.application.service.token;

import com.msashop.common.web.exception.AuthErrorCode;
import com.msashop.common.web.exception.UnauthorizedException;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenParser {

    public String extractTokenId(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException(AuthErrorCode.AUTH_REFRESH_MISSING);
        }
        int idx = rawRefreshToken.indexOf('.');
        if (idx <= 0 || idx == rawRefreshToken.length() - 1) { // '.'이 처음/끝이면 invalid
            throw new UnauthorizedException(AuthErrorCode.AUTH_REFRESH_INVALID_FORMAT);
        }

        String tokenId = rawRefreshToken.substring(0, idx);

        return tokenId;
    }



}
