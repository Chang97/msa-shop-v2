package com.msashop.common.web.filter;

import com.msashop.common.web.trace.TraceIdConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * HTTP 요청마다 추적용 traceId를 확보하는 서블릿 필터다.
 * 요청 헤더에 {@value TraceIdConstants#TRACE_HEADER}가 있으면 그대로 사용하고, 없으면 새 UUID를 만든다.
 * 확보된 traceId는 MDC와 응답 헤더에 넣어 로그와 에러 응답을 연결할 수 있게 한다.
 */
public class TraceIdFilter extends OncePerRequestFilter {

    /**
     * 요청 단위 traceId를 MDC에 심고 응답 헤더에도 동일한 값을 내려준다.
     * 처리 종료 후에는 MDC를 정리해서 다음 요청으로 값이 새지 않게 한다.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = request.getHeader(TraceIdConstants.TRACE_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        MDC.put(TraceIdConstants.MDC_KEY, traceId);
        response.setHeader(TraceIdConstants.TRACE_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TraceIdConstants.MDC_KEY);
        }
    }
}
