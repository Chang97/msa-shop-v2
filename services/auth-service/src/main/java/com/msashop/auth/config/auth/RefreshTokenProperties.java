package com.msashop.auth.config.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Refresh Token ?ㅼ젙 媛?諛붿씤??
 *
 * app.auth.refresh-token.ttl-seconds
 * - Refresh Token 留뚮즺(珥?
 * - ?? 7??= 604800
 */
@ConfigurationProperties(prefix = "app.auth.refresh-token")
public record RefreshTokenProperties (
        long ttlSeconds
) {}
