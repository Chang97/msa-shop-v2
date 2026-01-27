package com.msashop.auth.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * auth-service 보안 설정(최소 구성)
 *
 * 역할:
 * - auth-service는 "발급/회전/폐기"만 담당
 * - access token 검증(Resource Server)은 Gateway에서 수행
 *
 * 정책:
 * - 세션/폼로그인/Basic 비활성화
 * - CSRF는 auth API에 대해서는 disable(토큰 기반 + refresh는 HttpOnly cookie)
 * - /api/auth/login, /api/auth/refresh, /api/auth/logout 만 공개
 * - 나머지 요청은 404/403로 막아 서비스 면적 축소
 *
 * 내부 서비스 호출(X-Internal-Secret) 같은 건 나중에 별도 필터로 추가할수도?
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // 세션 미사용
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 기본 로그인/베이직 인증 끄기 (이거 안 끄면 의도치 않게 401 유발하는 경우 많음)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable) // /api/auth/logout 직접 구현

                // CSRF: auth API는 stateless + cookie refresh지만 SameSite/HttpOnly로 방어
                .csrf(AbstractHttpConfigurer::disable)

                // CORS는 gateway에서 처리
                .cors(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        // 로그인/갱신/로그아웃
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                        // 운영 필요 시
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // 그 외는 전부 차단 (auth-service의 책임 범위 축소)
                        .anyRequest().denyAll()
                )
                .build();
    }
}
