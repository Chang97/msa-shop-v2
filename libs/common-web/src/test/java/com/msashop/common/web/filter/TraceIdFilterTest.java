package com.msashop.common.web.filter;

import com.msashop.common.web.trace.TraceIdConstants;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    /**
     * 테스트 간 MDC 오염을 막기 위해 매 실행 후 traceId를 비운다.
     */
    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    /**
     * 요청 헤더에 traceId가 없으면 새 값을 만들고, 응답 헤더에 담은 뒤 요청 종료 후 MDC를 정리하는지 검증한다.
     */
    @Test
    void generatesTraceIdAndClearsMdcAfterRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceIdInsideChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) ->
                traceIdInsideChain.set(MDC.get(TraceIdConstants.MDC_KEY)));

        assertThat(traceIdInsideChain.get()).isNotBlank();
        assertThat(response.getHeader(TraceIdConstants.TRACE_HEADER)).isEqualTo(traceIdInsideChain.get());
        assertThat(MDC.get(TraceIdConstants.MDC_KEY)).isNull();
    }

    /**
     * 요청 헤더에 기존 traceId가 있으면 새로 만들지 않고 그대로 재사용하는지 검증한다.
     */
    @Test
    void reusesExistingTraceIdHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        request.addHeader(TraceIdConstants.TRACE_HEADER, "trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceIdInsideChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) ->
                traceIdInsideChain.set(MDC.get(TraceIdConstants.MDC_KEY)));

        assertThat(traceIdInsideChain.get()).isEqualTo("trace-123");
        assertThat(response.getHeader(TraceIdConstants.TRACE_HEADER)).isEqualTo("trace-123");
    }
}
