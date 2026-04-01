package com.msashop.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.common.web.response.ErrorResponse;
import com.msashop.common.web.trace.TraceIdConstants;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Rate limit 거부 시 내려줄 429 응답 전용 writer.
 *
 * 이 클래스를 따로 둔 이유:
 * - JsonRateLimiterGatewayFilterFactory 안에 응답 생성 로직까지 같이 두면
 *   "rate limit 판단"과 "에러 응답 작성" 책임이 섞여서 읽기 어려워진다.
 * - 그래서 이 클래스는 오직
 *   1. ErrorResponse를 만들고
 *   2. JSON으로 직렬화해서
 *   3. WebFlux response에 쓰는 일만 담당한다.
 */
@Component
public class GatewayRateLimitResponseWriter {

    private final ObjectMapper objectMapper;

    public GatewayRateLimitResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ErrorResponse buildErrorResponse(ServerWebExchange exchange) {
        // gateway도 common-web의 ErrorResponse 포맷을 그대로 사용한다.
        // 다만 MVC 예외 처리기가 아니라 WebFlux 응답을 직접 쓰므로 writer를 별도로 둔다.
        return new ErrorResponse(
                CommonErrorCode.COMMON_TOO_MANY_REQUESTS.code(),
                CommonErrorCode.COMMON_TOO_MANY_REQUESTS.defaultMessage(),
                CommonErrorCode.COMMON_TOO_MANY_REQUESTS.status(),
                Instant.now(),
                exchange.getRequest().getURI().getPath(),
                resolveTraceId(exchange),
                null
        );
    }

    public Mono<Void> write(ServerWebExchange exchange) {
        try {
            ErrorResponse body = buildErrorResponse(exchange);
            byte[] json = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(json);

            // 429 상태와 JSON content-type을 명시하고 body를 바로 내려준다.
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            exchange.getResponse().getHeaders().setContentLength(json.length);

            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            // 직렬화 같은 예상 밖 오류가 나더라도 최소한 status는 429로 유지한다.
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
    }

    private String resolveTraceId(ServerWebExchange exchange) {
        // 응답 헤더에 이미 traceId가 있으면 그 값을 우선 사용하고,
        // 아직 없다면 요청 헤더의 traceId를 fallback으로 사용한다.
        String traceId = exchange.getResponse().getHeaders().getFirst(TraceIdConstants.TRACE_HEADER);
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }

        traceId = exchange.getRequest().getHeaders().getFirst(TraceIdConstants.TRACE_HEADER);
        return (traceId == null || traceId.isBlank()) ? null : traceId;
    }
}
