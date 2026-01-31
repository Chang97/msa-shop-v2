package com.msashop.order.common.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class GatewayAuthenticationToken extends AbstractAuthenticationToken {

    private final String userId;

    public GatewayAuthenticationToken(String userId, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.userId = userId;
        setAuthenticated(true);
    }

    public Long getUserId() {
        return Long.parseLong(userId);
    }

    @Override
    public Object getCredentials() {
        return ""; // header 기반, credentials 없음
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }

}
