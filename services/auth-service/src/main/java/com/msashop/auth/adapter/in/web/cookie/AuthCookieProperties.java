package com.msashop.auth.adapter.in.web.cookie;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.cookie")
public record AuthCookieProperties(
        boolean secure,
        String sameSite,
        String path
) {
}

