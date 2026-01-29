package com.msashop.auth.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 내부 호출 보호용 간단한 시크릿 헤더 필터.
 * - /internal/** 요청에 대해 X-Internal-Secret 헤더를 검사.
 */
@Component
public class InternalSecretFilter extends OncePerRequestFilter {

    @Value("${security.internal.header-name:X-Internal-Secret}")
    private String headerName;

    @Value("${security.internal.service-secret:local-internal-secret}")
    private String expected;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (!uri.startsWith("/internal/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(headerName);
        if (expected.equals(header)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.FORBIDDEN.value());
    }
}

