package com.msashop.auth.command.application.service.token;

import org.springframework.stereotype.Component;

@Component
public class RefreshTokenParser {

    public String extractTokenId(String rawRefreshToken) {
        if (rawRefreshToken == null) throw new IllegalArgumentException("refreshToken is null");
        int idx = rawRefreshToken.indexOf('.');
        if (idx <= 0) throw new IllegalArgumentException("Invalid refresh token format");
        return rawRefreshToken.substring(0, idx);
    }
}
