package com.msashop.common.web.trace;

/**
 * traceId 헤더명과 MDC 키를 공통으로 관리한다.
 */
public final class TraceIdConstants {

    public static final String TRACE_HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";

    private TraceIdConstants() {
    }
}
