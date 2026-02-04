package com.msashop.payment.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GatewayAuthHeaderFilter extends OncePerRequestFilter {
    private final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = request.getHeader("X-User-Id");
        String rolesHeader = request.getHeader("X-Roles");

        log.debug("[PS] userId : {}, roles: {}", userId, rolesHeader);

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            if (userId != null && !userId.isBlank()) {
                List<SimpleGrantedAuthority> authorities = parseAuthorities(rolesHeader);
                var auth = new GatewayAuthenticationToken(userId, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> parseAuthorities(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) return List.of();
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}

