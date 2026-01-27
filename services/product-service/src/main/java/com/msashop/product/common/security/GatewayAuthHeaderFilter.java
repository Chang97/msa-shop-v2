package com.msashop.product.common.security;

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
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // CORS preflight는 그냥 통과
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = request.getHeader("X-User-Id");
        String rolesHeader = request.getHeader("X-Roles"); // ex) ROLE_USER,ROLE_ADMIN

        log.debug("[PS] userId : {}, roles: rolesHeader : {}", userId, rolesHeader);

        // 이미 인증된 컨텍스트가 있으면 재설정하지 않음
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            if (userId != null && !userId.isBlank()) {
                List<SimpleGrantedAuthority> authorities = parseAuthorities(rolesHeader);
                var auth = new HeaderAuthenticationToken(userId, authorities);
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
