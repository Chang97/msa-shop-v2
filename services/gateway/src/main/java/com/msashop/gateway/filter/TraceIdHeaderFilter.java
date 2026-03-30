package com.msashop.gateway.filter;

import com.msashop.common.web.trace.TraceIdConstants;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Gateway 진입 시점에 traceId를 확보해서 downstream 요청과 응답 헤더에 함께 실어준다.
 */
@Component
public class TraceIdHeaderFilter implements WebFilter, Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TraceIdConstants.TRACE_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        String resolvedTraceId = traceId;

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> headers.set(TraceIdConstants.TRACE_HEADER, resolvedTraceId))
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(request)
                .build();

        mutatedExchange.getResponse().getHeaders().set(TraceIdConstants.TRACE_HEADER, resolvedTraceId);
        return chain.filter(mutatedExchange);
    }
}
