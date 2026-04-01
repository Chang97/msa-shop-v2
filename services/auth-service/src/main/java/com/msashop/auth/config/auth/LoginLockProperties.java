package com.msashop.auth.config.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.login-lock")
public record LoginLockProperties(
        String keyPrefix,
        long maxFailures,
        long lockSeconds
) {
}
