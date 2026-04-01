package com.msashop.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.web.response.ErrorResponse;
import com.msashop.common.web.trace.TraceIdConstants;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRateLimitResponseWriterTest {

    private final GatewayRateLimitResponseWriter writer =
            new GatewayRateLimitResponseWriter(new ObjectMapper());

    @Test
    void should_build_common_rate_limit_error_response() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/login")
                        .header(TraceIdConstants.TRACE_HEADER, "trace-123")
                        .build()
        );

        ErrorResponse body = writer.buildErrorResponse(exchange);

        assertThat(body.code()).isEqualTo("COM_429");
        assertThat(body.message()).isEqualTo("요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
        assertThat(body.status()).isEqualTo(429);
        assertThat(body.path()).isEqualTo("/api/auth/login");
        assertThat(body.traceId()).isEqualTo("trace-123");
    }
}
