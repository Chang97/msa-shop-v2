package com.msashop.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;
import org.springframework.http.HttpStatus;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                // stateless gateway
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                // CORS: globalcors 사용 시에도 withDefaults 권장
                .cors(Customizer.withDefaults())

                // 인증/인가 실패 시 상태코드 명확화 (디버깅 도움)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)) // 401
                        .accessDeniedHandler(new HttpStatusServerAccessDeniedHandler(HttpStatus.FORBIDDEN)) // 403
                )

                .authorizeExchange(ex -> ex
                        // preflight
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 공개 API
                        .pathMatchers("/api/auth/**").permitAll()
                        .pathMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // 나머지 전부 JWT 필요
                        .anyExchange().authenticated()
                )

                // Authorization: Bearer <token> 검증
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))

                .build();
    }
}
