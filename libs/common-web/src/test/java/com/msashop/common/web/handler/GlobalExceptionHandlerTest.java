package com.msashop.common.web.handler;

import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.common.web.response.ErrorResponse;
import com.msashop.common.web.trace.TraceIdConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /**
     * 테스트 간 MDC 오염을 막기 위해 매 실행 후 traceId를 비운다.
     */
    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    /**
     * 비즈니스 예외 응답에 상태 코드, 요청 경로, traceId가 함께 담기는지 검증한다.
     */
    @Test
    void includesPathAndTraceIdForBusinessException() {
        MDC.put(TraceIdConstants.MDC_KEY, "trace-123");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/1");

        var response = handler.handleBusiness(
                new BusinessException(CommonErrorCode.COMMON_CONFLICT, "충돌입니다."),
                request
        );

        ErrorResponse body = response.getBody();
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo(CommonErrorCode.COMMON_CONFLICT.code());
        assertThat(body.message()).isEqualTo("충돌입니다.");
        assertThat(body.path()).isEqualTo("/api/orders/1");
        assertThat(body.traceId()).isEqualTo("trace-123");
    }

    /**
     * 바인딩 검증 실패가 fieldErrors 목록으로 변환되는지 검증한다.
     */
    @Test
    void mapsFieldErrorsForBindException() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "이메일 형식이 올바르지 않습니다."));
        BindException ex = new BindException(bindingResult);

        var response = handler.handleBind(ex, request);

        ErrorResponse body = response.getBody();
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(body).isNotNull();
        assertThat(body.fieldErrors()).hasSize(1);
        assertThat(body.fieldErrors().getFirst().field()).isEqualTo("email");
        assertThat(body.fieldErrors().getFirst().message()).isEqualTo("이메일 형식이 올바르지 않습니다.");
    }

    /**
     * 예상하지 못한 예외는 내부 메시지를 숨기고 공통 500 응답으로 변환하는지 검증한다.
     */
    @Test
    void hidesInternalMessageForUnknownException() {
        MDC.put(TraceIdConstants.MDC_KEY, "trace-500");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/1");

        var response = handler.handleUnknown(new IllegalStateException("db down"), request);

        ErrorResponse body = response.getBody();
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo(CommonErrorCode.COMMON_INTERNAL_ERROR.defaultMessage());
        assertThat(body.traceId()).isEqualTo("trace-500");
    }
}
