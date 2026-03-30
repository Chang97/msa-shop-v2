package com.msashop.gateway.filter;

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

/**
 * 게이트웨이 인증이 끝난 뒤 JWT의 사용자 정보를 내부 서비스 전달용 헤더로 변환한다.
 */
@Component
public class AuthContextHeaderFilter implements WebFilter, Ordered {
    /**
     * Security WebFilterChain 이후에 실행되도록 우선순위를 가장 뒤로 둔다.
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * 인증된 JWT에서 userId와 roles를 꺼내 downstream 서비스가 읽을 수 있는 헤더로 주입한다.
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return exchange.getPrincipal()
                .ofType(Authentication.class)
                .flatMap(auth -> {
                    if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
                        return Mono.just(exchange);
                    }
                    if (!jwtAuth.isAuthenticated()) {
                        return Mono.just(exchange);
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
                    return Mono.just(exchange.mutate().request(mutated).build());
                })
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }
}
