package com.msashop.common.web.handler;

import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.common.web.exception.ErrorCode;
import com.msashop.common.web.response.ErrorResponse;
import com.msashop.common.web.trace.TraceIdConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/**
 * 모든 서비스에서 공통으로 사용하는 전역 예외 처리기.
 *
 * 비즈니스 예외는 정의된 에러 코드 기준으로 응답을 만들고,
 * 예상하지 못한 예외는 공통 500 응답으로 변환한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 애플리케이션에서 의도적으로 던진 비즈니스 예외를 처리한다.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
        ErrorCode ec = ex.errorCode();

        log.warn("Business exception: method={}, path={}, code={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                ec.code(),
                ex.getMessage());

        ErrorResponse body = new ErrorResponse(
                ec.code(),
                ex.getMessage(),
                ec.status(),
                Instant.now(),
                request.getRequestURI(),
                traceId(),
                null
        );
        return ResponseEntity.status(ec.status()).body(body);
    }

    /**
     * Spring Security 인증 실패를 공통 401 응답으로 변환한다.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication exception: method={}, path={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());

        ErrorResponse body = new ErrorResponse(
                CommonErrorCode.COMMON_UNAUTHORIZED.code(),
                CommonErrorCode.COMMON_UNAUTHORIZED.defaultMessage(),
                CommonErrorCode.COMMON_UNAUTHORIZED.status(),
                Instant.now(),
                request.getRequestURI(),
                traceId(),
                null
        );
        return ResponseEntity.status(CommonErrorCode.COMMON_UNAUTHORIZED.status()).body(body);
    }

    /**
     * Spring Security 인가 실패를 공통 403 응답으로 변환한다.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: method={}, path={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());

        ErrorResponse body = new ErrorResponse(
                CommonErrorCode.COMMON_FORBIDDEN.code(),
                CommonErrorCode.COMMON_FORBIDDEN.defaultMessage(),
                CommonErrorCode.COMMON_FORBIDDEN.status(),
                Instant.now(),
                request.getRequestURI(),
                traceId(),
                null
        );
        return ResponseEntity.status(CommonErrorCode.COMMON_FORBIDDEN.status()).body(body);
    }

    /**
     * RequestBody 기반 검증 실패를 처리하고 필드별 오류를 함께 내려준다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldErrorDetail> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .toList();

        log.warn("Validation exception: method={}, path={}, fieldErrors={}",
                request.getMethod(),
                request.getRequestURI(),
                fieldErrors);

        ErrorResponse body = new ErrorResponse(
                CommonErrorCode.COMMON_VALIDATION.code(),
                CommonErrorCode.COMMON_VALIDATION.defaultMessage(),
                CommonErrorCode.COMMON_VALIDATION.status(),
                Instant.now(),
                request.getRequestURI(),
                traceId(),
                fieldErrors
        );
        return ResponseEntity.status(CommonErrorCode.COMMON_VALIDATION.status()).body(body);
    }

    /**
     * form/model attribute 바인딩 검증 실패를 처리한다.
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBind(BindException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldErrorDetail> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .toList();

        log.warn("Bind exception: method={}, path={}, fieldErrors={}",
                request.getMethod(),
                request.getRequestURI(),
                fieldErrors);

        ErrorResponse body = new ErrorResponse(
                CommonErrorCode.COMMON_VALIDATION.code(),
                CommonErrorCode.COMMON_VALIDATION.defaultMessage(),
                CommonErrorCode.COMMON_VALIDATION.status(),
                Instant.now(),
                request.getRequestURI(),
                traceId(),
                fieldErrors
        );
        return ResponseEntity.status(CommonErrorCode.COMMON_VALIDATION.status()).body(body);
    }

    /**
     * RequestParam, PathVariable 등 제약 조건 검증 실패를 처리한다.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldErrorDetail> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(v -> new ErrorResponse.FieldErrorDetail(
                        v.getPropertyPath().toString(),
                        v.getMessage()
                ))
                .toList();

        log.warn("Constraint violation: method={}, path={}, fieldErrors={}",
                request.getMethod(),
                request.getRequestURI(),
                fieldErrors);

        ErrorResponse body = new ErrorResponse(
                CommonErrorCode.COMMON_VALIDATION.code(),
                CommonErrorCode.COMMON_VALIDATION.defaultMessage(),
                CommonErrorCode.COMMON_VALIDATION.status(),
                Instant.now(),
                request.getRequestURI(),
                traceId(),
                fieldErrors
        );
        return ResponseEntity.status(CommonErrorCode.COMMON_VALIDATION.status()).body(body);
    }

    /**
     * 처리되지 않은 예외를 공통 500 응답으로 변환한다.
     *
     * 내부 상세 메시지는 응답에 숨기고, 로그에는 스택트레이스를 남긴다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception: method={}, path={}, type={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getClass().getName(),
                ex.getMessage(),
                ex);

        ErrorResponse body = new ErrorResponse(
                CommonErrorCode.COMMON_INTERNAL_ERROR.code(),
                CommonErrorCode.COMMON_INTERNAL_ERROR.defaultMessage(),
                CommonErrorCode.COMMON_INTERNAL_ERROR.status(),
                Instant.now(),
                request.getRequestURI(),
                traceId(),
                null
        );
        return ResponseEntity.status(CommonErrorCode.COMMON_INTERNAL_ERROR.status()).body(body);
    }

    /**
     * Spring FieldError를 API 응답용 필드 오류 형식으로 변환한다.
     */
    private ErrorResponse.FieldErrorDetail toFieldError(FieldError fieldError) {
        return new ErrorResponse.FieldErrorDetail(
                fieldError.getField(),
                fieldError.getDefaultMessage()
        );
    }

    /**
     * MDC에 저장된 traceId를 읽어 응답과 로그를 연결한다.
     */
    private String traceId() {
        return MDC.get(TraceIdConstants.MDC_KEY);
    }
}
