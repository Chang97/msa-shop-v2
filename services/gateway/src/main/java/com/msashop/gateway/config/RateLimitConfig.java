package com.msashop.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    /**
     * 비인증 요청용 rate limit key resolver.
     * 로그인/토큰 재발급처럼 사용자 식별 전 요청은 클라이언트 IP를 기준으로 제한한다.
     */
    @Bean
    @Primary
    public KeyResolver clientIpKeyResolver() {
        return exchange -> Mono.just("ip:" + extractClientIp(exchange.getRequest()));
    }

    /**
     * 인증 요청용 rate limit key resolver.
     * JWT가 검증된 요청은 사용자 식별자를 우선 사용하고,
     * 인증 정보가 없으면 IP 기준으로 fallback 한다.
     */
    @Bean
    public KeyResolver authenticatedUserKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .cast(Authentication.class)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .filter(userId -> !userId.isBlank())
                .map(userId -> "user:" + userId)
                .defaultIfEmpty("ip:" + extractClientIp(exchange.getRequest()));
    }

    /**
     * 프록시 뒤 운영환경을 고려해 X-Forwarded-For 헤더를 우선 사용한다.
     * 헤더가 없으면 gateway가 직접 본 remote address를 사용한다.
     */
    private String extractClientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // X-Forwarded-For는 "client, proxy1, proxy2" 형태일 수 있으므로 첫 번째 IP를 사용한다.
            String firstIp = forwardedFor.split(",")[0].trim();
            if (!firstIp.isEmpty()) {
                return firstIp;
            }
        }

        var remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        // 테스트나 특수 환경에서 주소를 식별할 수 없는 경우를 위한 fallback 값이다.
        return "unknown";
    }
}
