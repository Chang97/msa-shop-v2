package com.msashop.payment.config.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "app.idempotency")
public record IdempotencyProperties(String keyPrefix, long ttlSeconds) {
}
