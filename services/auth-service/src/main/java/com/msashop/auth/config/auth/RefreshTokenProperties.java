package com.msashop.auth.config.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Refresh Token 설정 값 바인딩.
 *
 * app.auth.refresh-token.ttl-seconds
 * - Refresh Token 만료(초)
 * - 예: 7일 = 604800
 */
@ConfigurationProperties(prefix = "app.auth.refresh-token")
public record RefreshTokenProperties (
        long ttlSeconds
) {}
