package com.msashop.auth.adapter.in.web.cookie;

import com.msashop.auth.config.auth.RefreshTokenProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 리프레시 토큰 쿠키를 생성하거나 만료 처리하는 팩토리다.
 */
@Component
@RequiredArgsConstructor
public class RefreshCookieFactory {
    public static final String REFRESH_COOKIE_NAME = "rt";
    private final RefreshTokenProperties refreshTokenProperties;
    private final AuthCookieProperties authCookieProperties;

    /**
     * 리프레시 토큰 값을 담은 HttpOnly 쿠키를 생성한다.
     */
    public ResponseCookie create(String refreshToken, boolean secure) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(authCookieProperties.secure())
                .path(authCookieProperties.path())
                .maxAge(Duration.ofSeconds(refreshTokenProperties.ttlSeconds()))
                .sameSite(authCookieProperties.sameSite())
                .build();
    }

    /**
     * 동일한 쿠키 이름으로 즉시 만료 쿠키를 내려 리프레시 토큰 쿠키를 삭제한다.
     */
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

