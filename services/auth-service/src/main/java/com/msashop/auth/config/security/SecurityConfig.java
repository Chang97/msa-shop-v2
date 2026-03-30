package com.msashop.auth.config.security;

import com.msashop.auth.common.security.GatewayAuthHeaderFilter;
import com.msashop.common.web.filter.TraceIdFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    GatewayAuthHeaderFilter gatewayAuthHeaderFilter() {
        return new GatewayAuthHeaderFilter();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            InternalSecretFilter internalSecretFilter,
            GatewayAuthHeaderFilter gatewayAuthHeaderFilter
    ) throws Exception {
        TraceIdFilter traceIdFilter = new TraceIdFilter();

        return http
                // stateless
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // disable default logins
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                // CSRF off (stateless + cookie refresh)
                .csrf(AbstractHttpConfigurer::disable)

                // CORS handled at gateway
                .cors(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        // internal endpoints are protected by InternalSecretFilter
                        .requestMatchers("/internal/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().denyAll()
                )

                .addFilterBefore(traceIdFilter, UsernamePasswordAuthenticationFilter.class)
                // protect /internal/** with secret header
                .addFilterAfter(internalSecretFilter, TraceIdFilter.class)
                .addFilterAfter(gatewayAuthHeaderFilter, InternalSecretFilter.class)

                .build();
    }
}
