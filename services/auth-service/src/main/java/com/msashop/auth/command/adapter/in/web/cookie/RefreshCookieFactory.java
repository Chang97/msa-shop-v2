package com.msashop.auth.command.adapter.in.web.cookie;

import com.msashop.auth.config.auth.RefreshTokenProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshCookieFactory {
    public static final String REFRESH_COOKIE_NAME = "rt";
    private final RefreshTokenProperties refreshTokenProperties;
    private final AuthCookieProperties authCookieProperties;

    public ResponseCookie create(String refreshToken, boolean secure) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(authCookieProperties.secure())
                .path(authCookieProperties.path())
                .maxAge(Duration.ofSeconds(refreshTokenProperties.ttlSeconds()))
                .sameSite(authCookieProperties.sameSite())
                .build();
    }

    public ResponseCookie delete(boolean secure) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(authCookieProperties.secure())
                .path(authCookieProperties.path())
                .maxAge(Duration.ZERO)
                .sameSite(authCookieProperties.sameSite())
                .build();
    }
}
