package com.msashop.order.config.security;

import com.msashop.common.web.filter.TraceIdFilter;
import com.msashop.order.common.security.GatewayAuthHeaderFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity // @PreAuthorize 사용
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            GatewayAuthHeaderFilter gatewayAuthHeaderFilter,
                                            InternalSecretFilter internalSecretFilter) throws Exception {
        TraceIdFilter traceIdFilter = new TraceIdFilter();

        return http
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // 허용: actuator 등
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/internal/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated()
                )

                // 헤더 기반 인증 필터 주입 (인증 필터보다 앞에 위치)
                .addFilterBefore(traceIdFilter, GatewayAuthHeaderFilter.class)
                .addFilterBefore(gatewayAuthHeaderFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(internalSecretFilter, GatewayAuthHeaderFilter.class)

                .build();
    }
}
