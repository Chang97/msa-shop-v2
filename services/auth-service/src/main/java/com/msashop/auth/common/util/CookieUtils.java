package com.msashop.auth.common.util;

import jakarta.servlet.http.Cookie;

public final class CookieUtils {
    private CookieUtils() {}

    public static Cookie httpOnlyCookie(String name, String value, int maxAgeSeconds,
                                        boolean secure, String sameSite, String path) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath(path);
        cookie.setMaxAge(maxAgeSeconds);
        return cookie;
    }
}
