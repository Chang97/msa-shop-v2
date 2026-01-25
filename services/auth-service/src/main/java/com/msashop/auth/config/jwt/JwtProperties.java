package com.msashop.auth.config.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * JWT 관련 설정을 application.yml에서 바인딩 받는 설정 객체.
 *
 * - prefix: security.jwt
 * - Resource 타입을 쓰면 classpath:/file:/ 등 다양한 위치의 키 파일을 유연하게 읽을 수 있음.
 *
 * 예)
 * security:
 *   jwt:
 *     issuer: auth-service
 *     access-token-ttl-seconds: 1800
 *     private-key-location: classpath:keys/jwt-private.pem
 *     public-key-location: classpath:keys/jwt-public.pem
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties (
    String issuer,
    long accessTokenTtlSeconds,
    Resource privateKeyLocation,
    Resource publicKeyLocation
) {}

