package com.msashop.user.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gateway가 전달한 인증 헤더를 읽어 SecurityContext에 사용자 정보를 적재하는 필터다.
 */
public class GatewayAuthHeaderFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GatewayAuthHeaderFilter.class);

    private static final String H_USER_ID = "X-User-Id";
    private static final String H_ROLES = "X-Roles";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        log.info("uri={}, X-User-Id={}, X-Roles={}",
                request.getRequestURI(),
                request.getHeader(H_USER_ID),
                request.getHeader(H_ROLES));

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        final String userIdHeader = request.getHeader(H_USER_ID);
        if (userIdHeader == null || userIdHeader.isBlank()) {
            log.debug("[GatewayAuthHeaderFilter] missing header: {}", H_USER_ID);
            filterChain.doFilter(request, response);
            return;
        }

        final Long userId;
        try {
            userId = Long.parseLong(userIdHeader.trim());
        } catch (NumberFormatException e) {
            log.warn("[GatewayAuthHeaderFilter] invalid userId header. value={}", userIdHeader);
            filterChain.doFilter(request, response);
            return;
        }

        final Set<String> roles = parseRoles(request.getHeader(H_ROLES));
        final var authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        final CurrentUser principal = new CurrentUser(userId, roles);
        final GatewayAuthenticationToken authentication =
                new GatewayAuthenticationToken(principal, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("after set: auth={}, principal={}, authorities={}",
                auth,
                auth == null ? null : auth.getPrincipal(),
                auth == null ? null : auth.getAuthorities());

        filterChain.doFilter(request, response);
    }

    /**
     * 쉼표로 전달된 역할 문자열을 Security authority 집합으로 변환한다.
     */
    private Set<String> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }
}
