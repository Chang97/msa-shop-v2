package com.msashop.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Gateway는 "1차 인증(토큰 유효성)"만 빠르게 컷해주는 역할이 핵심.
 * - 외부 요청은 모두 Gateway를 지나감
 * - JWT 검증(서명/만료) 실패 시 여기서 바로 401
 * - 공개 경로(/api/auth/**)는 인증 없이 통과
 * - 서비스 내부에서는 더 세밀한 권한 체크(@PreAuthorize)를 수행
 */
@Configuration
public class SecurityConfig {

    /**
     * WebFlux 기반 Spring Security 설정
     * authorizeExchange:
     *  - permitAll: 로그인/토큰발급 등 공개 API
     *  - authenticated: 그 외는 JWT 필요
     *
     * oauth2ResourceServer().jwt():
     *  - Authorization: Bearer <token>을 자동 인식하여 검증
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                // API Gateway는 세션 기반이 아니므로 CSRF는 보통 끔(토큰 기반)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> {})
                // 경로별 접근 제어
                .authorizeExchange(ex -> ex
                        // 인증 없이 열어둘 엔드포인트
                        .pathMatchers("/api/auth/**").permitAll()
                        // 그 외는 전부 인증 필요
                        .anyExchange().authenticated()
                )
                // JWT 검증 활성화 (Resource Server 표준)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .build();
    }

}
