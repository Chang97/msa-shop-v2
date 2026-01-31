package com.msashop.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthContextHeaderFilter implements WebFilter, Ordered {
    private final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());
    @Override
    public int getOrder() {
        // SecurityWebFilterChain 이후에 실행되게 적당히 뒤로
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        return exchange.getPrincipal() // Mono<Principal>
                .ofType(Authentication.class) // Principal -> Authentication 인 것만 통과
                .flatMap(auth -> {

                    if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
                        return chain.filter(exchange);
                    }
                    if (!jwtAuth.isAuthenticated()) {
                        return chain.filter(exchange);
                    }

                    Jwt jwt = jwtAuth.getToken();
                    String userId = jwt.getSubject();

                    List<String> roles = jwt.getClaimAsStringList("roles");
                    String rolesValue = (roles == null || roles.isEmpty()) ? "" : String.join(",", roles);

                    ServerHttpRequest mutated = exchange.getRequest().mutate()
                            .headers(h -> {
                                h.set("X-User-Id", userId);
                                h.set("X-Roles", rolesValue);
                            })
                            .build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                })
                // principal이 없거나(Authentication 아님) flatMap이 실행되지 않으면 그냥 통과
                .switchIfEmpty(chain.filter(exchange));
    }
}
