package com.msashop.auth.config.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * JWT 愿???ㅼ젙??application.yml?먯꽌 諛붿씤??諛쏅뒗 ?ㅼ젙 媛앹껜.
 *
 * - prefix: security.jwt
 * - Resource ??낆쓣 ?곕㈃ classpath:/file:/ ???ㅼ뼇???꾩튂?????뚯씪???좎뿰?섍쾶 ?쎌쓣 ???덉쓬.
 *
 * ??
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

