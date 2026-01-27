package com.msashop.user.common.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * 표준 Authentication 구현체.
 *
 * - Spring Security는 SecurityContextHolder에 들어있는 Authentication을 기준으로
 *   인가(@PreAuthorize, authorizeHttpRequests 등)를 수행한다.
 * - principal 타입을 CurrentUser로 고정하기 위해 커스텀 토큰을 사용한다.
 *
 * 포인트:
 * - credentials는 내부 서비스에서 필요 없으므로 빈 문자열.
 * - setAuthenticated(true)로 인증 완료 상태로 올린다.
 *
 * 주의:
 * - 외부에서 직접 생성하지 말고, GatewayAuthHeaderFilter 내부에서만 생성하는 것을 권장.
 */
public class GatewayAuthenticationToken extends AbstractAuthenticationToken {

    private final CurrentUser principal;

    public GatewayAuthenticationToken(CurrentUser principal,
                                      Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;

        // 내부망에서 Gateway를 신뢰한다는 전제 하에, 헤더 검증이 끝나면 인증 완료로 세팅
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        // 내부 서비스는 JWT를 직접 다루지 않으므로 credentials는 불필요
        return "";
    }

    @Override
    public CurrentUser getPrincipal() {
        return principal;
    }
}
