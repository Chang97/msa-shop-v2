package com.msashop.auth.common.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Gateway 헤더 기반 인증 결과를 SecurityContext에 담기 위한 Authentication 구현체다.
 */
public class GatewayAuthenticationToken extends AbstractAuthenticationToken {

    private final CurrentUser principal;

    public GatewayAuthenticationToken(
            CurrentUser principal,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public CurrentUser getPrincipal() {
        return principal;
    }
}
