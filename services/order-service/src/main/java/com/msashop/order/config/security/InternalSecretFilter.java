package com.msashop.order.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalSecretFilter extends OncePerRequestFilter {

    @Value("${security.internal.header-name:X-Internal-Secret}")
    private String headerName;

    @Value("${security.internal.service-secret:local-internal-secret}")
    private String expected;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

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

