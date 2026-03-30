package com.msashop.gateway.filter;

import com.msashop.common.web.trace.TraceIdConstants;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdHeaderFilterTest {

    private final TraceIdHeaderFilter filter = new TraceIdHeaderFilter();

    @Test
    void generatesTraceIdWhenHeaderIsMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders").build()
        );
        AtomicReference<ServerWebExchange> seenExchange = new AtomicReference<>();
        WebFilterChain chain = ex -> {
            seenExchange.set(ex);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        String requestTraceId = seenExchange.get().getRequest().getHeaders().getFirst(TraceIdConstants.TRACE_HEADER);
        String responseTraceId = exchange.getResponse().getHeaders().getFirst(TraceIdConstants.TRACE_HEADER);

        assertThat(requestTraceId).isNotBlank();
        assertThat(responseTraceId).isEqualTo(requestTraceId);
    }

    @Test
    void reusesExistingTraceIdHeader() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .header(TraceIdConstants.TRACE_HEADER, "trace-123")
                        .build()
        );
        AtomicReference<ServerWebExchange> seenExchange = new AtomicReference<>();
        WebFilterChain chain = ex -> {
            seenExchange.set(ex);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        String requestTraceId = seenExchange.get().getRequest().getHeaders().getFirst(TraceIdConstants.TRACE_HEADER);
        String responseTraceId = exchange.getResponse().getHeaders().getFirst(TraceIdConstants.TRACE_HEADER);

        assertThat(requestTraceId).isEqualTo("trace-123");
        assertThat(responseTraceId).isEqualTo("trace-123");
    }
}
