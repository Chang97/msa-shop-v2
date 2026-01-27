package com.msashop.user.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 표준: Gateway가 전달한 헤더를 읽어 Spring Security 인증 컨텍스트로 승격하는 필터.
 *
 * 흐름:
 * 1) Gateway(WebFlux)가 JWT 검증 후:
 *    - X-User-Id: {sub}
 *    - X-Roles: ROLE_USER,ROLE_ADMIN ...
 *    헤더를 붙여 내부 서비스로 라우팅
 *
 * 2) 내부 서비스(MVC)는 이 필터에서 위 헤더를 읽어:
 *    - CurrentUser 생성
 *    - GrantedAuthority 생성
 *    - SecurityContextHolder에 Authentication 저장
 *
 * 운영 주의:
 * - 이 방식은 "Gateway를 신뢰"한다는 전제가 있으므로
 *   내부 서비스는 외부에서 직접 접근 불가(네트워크 레벨 차단) 또는
 *   내부 서비스가 "Gateway 전용 헤더 서명/시크릿" 같은 추가 검증이 필요할 수 있다.
 *
 * 디버깅 포인트:
 * - userId가 null로 들어오면: (1) Gateway 헤더 누락 (2) 이 필터가 등록/실행 안 됨 (3) SecurityContext 덮어쓰기 문제
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // 가능한 앞단에서 세팅하여 뒤 인가 단계가 활용하도록 한다.
public class GatewayAuthHeaderFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GatewayAuthHeaderFilter.class);

    private static final String H_USER_ID = "X-User-Id";
    private static final String H_ROLES = "X-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        log.info("uri={}, X-User-Id={}, X-Roles={}",
                request.getRequestURI(),
                request.getHeader("X-User-Id"),
                request.getHeader("X-Roles"));


        // (중요) 이미 인증이 들어있으면 중복 세팅하지 않는다.
        // - 다른 보안 구성(JWT resource server 등)이 섞여 있을 때 덮어쓰기 방지
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        final String userIdHeader = request.getHeader(H_USER_ID);

        // Gateway가 헤더를 안 붙이면 "인증되지 않은 요청"으로 취급한다.
        // - 여기서 바로 401을 내려도 되지만, 표준은 SecurityConfig에서 authenticated()로 막게 둔다.
        if (userIdHeader == null || userIdHeader.isBlank()) {
            log.debug("[GatewayAuthHeaderFilter] missing header: {}", H_USER_ID);
            filterChain.doFilter(request, response);
            return;
        }

        final Long userId;
        try {
            userId = Long.parseLong(userIdHeader.trim());
        } catch (NumberFormatException e) {
            // 헤더 포맷이 깨졌으면 인증 승격하지 않는다.
            log.warn("[GatewayAuthHeaderFilter] invalid userId header. value={}", userIdHeader);
            filterChain.doFilter(request, response);
            return;
        }

        final String rolesHeader = request.getHeader(H_ROLES);
        final Set<String> roles = parseRoles(rolesHeader);

        // Spring Security 인가 체크(@PreAuthorize hasAuthority 등)에서 사용하는 authorities 생성
        // - roles 문자열은 반드시 ROLE_ 접두 포함 형태로 통일
        final var authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        final CurrentUser principal = new CurrentUser(userId, roles);
        final GatewayAuthenticationToken authentication =
                new GatewayAuthenticationToken(principal, authorities);

        // SecurityContext에 세팅 (이후부터 @AuthenticationPrincipal 로 주입 가능)
        SecurityContextHolder.getContext().setAuthentication(authentication);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("after set: auth={}, principal={}, authorities={}",
                auth,
                auth == null ? null : auth.getPrincipal(),
                auth == null ? null : auth.getAuthorities());


        filterChain.doFilter(request, response);
    }

    /**
     * X-Roles 헤더 파싱 표준:
     * - "ROLE_USER,ROLE_ADMIN" 같은 콤마 구분 문자열
     * - 공백 trim
     * - 빈 토큰 제거
     * - Set으로 중복 제거
     */
    private Set<String> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) return Set.of();

        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }
}
