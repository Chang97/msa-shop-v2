package com.msashop.auth.config.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * JWT 발급에 필요한 설정값을 바인딩하는 프로퍼티 객체다.
 * <p>
 * prefix: security.jwt
 * <p>
 * 예시:
 * security:
 *   jwt:
 *     issuer: auth-service
 *     access-token-ttl-seconds: 1800
 *     private-key-location: classpath:keys/jwt-private.pem
 *     public-key-location: classpath:keys/jwt-public.pem
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        String issuer,
        long accessTokenTtlSeconds,
        Resource privateKeyLocation,
        Resource publicKeyLocation
) {
}
