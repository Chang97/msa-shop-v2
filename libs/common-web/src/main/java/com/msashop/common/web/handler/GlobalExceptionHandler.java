package com.msashop.common.web.handler;

import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.common.web.exception.ErrorCode;
import com.msashop.common.web.response.ErrorResponse;

import jakarta.validation.ConstraintViolationException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * 모든 서비스에 적용되는 공통 예외 처리기.
 *
 * 주의:
 * - 서비스를 모르는 공용 코드이므로 서비스별 enum을 직접 참조하면 안 된다.
 * - unknown 예외는 COM_500으로 통일한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 서비스 코드 기반 비즈니스 예외 처리.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        ErrorCode ec = ex.errorCode();
        ErrorResponse body = new ErrorResponse(
                ec.code(),
                ex.getMessage(),
                ec.status(),
                Instant.now()
        );
        return ResponseEntity.status(ec.status()).body(body);
    }

    /**
     * 인증 실패 (Spring Security AuthenticationException 계열).
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        ErrorResponse body = new ErrorResponse(
                CommonErrorCode.COMMON_UNAUTHORIZED.code(),
                ex.getMessage(),
                CommonErrorCode.COMMON_UNAUTHORIZED.status(),
                Instant.now()
        );
        return ResponseEntity.status(CommonErrorCode.COMMON_UNAUTHORIZED.status()).body(body);
    }

    /**
     * 인가 실패 (권한 부족): 403 반환.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse body = new ErrorResponse(
                CommonErrorCode.COMMON_FORBIDDEN.code(),
                ex.getMessage(),
                CommonErrorCode.COMMON_FORBIDDEN.status(),
                Instant.now()
        );
        return ResponseEntity.status(CommonErrorCode.COMMON_FORBIDDEN.status()).body(body);
    }

    /**
     * @Valid / @Validated 입력 검증 (RequestBody).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgNotValid(MethodArgumentNotValidException ex) {
        ErrorResponse body = new ErrorResponse(
                CommonErrorCode.COMMON_VALIDATION.code(),
                CommonErrorCode.COMMON_VALIDATION.defaultMessage(),
                CommonErrorCode.COMMON_VALIDATION.status(),
                Instant.now()
        );
        return ResponseEntity.status(CommonErrorCode.COMMON_VALIDATION.status()).body(body);
    }

    /**
     * @ModelAttribute / form binding 검증.
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBind(BindException ex) {
        ErrorResponse body = new ErrorResponse(
                CommonErrorCode.COMMON_VALIDATION.code(),
                CommonErrorCode.COMMON_VALIDATION.defaultMessage(),
                CommonErrorCode.COMMON_VALIDATION.status(),
                Instant.now()
        );
        return ResponseEntity.status(CommonErrorCode.COMMON_VALIDATION.status()).body(body);
    }

    /**
     * 파라미터 단위 검증(@Validated on @RequestParam, @PathVariable 등)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        ErrorResponse body = new ErrorResponse(
                CommonErrorCode.COMMON_VALIDATION.code(),
                CommonErrorCode.COMMON_VALIDATION.defaultMessage(),
                CommonErrorCode.COMMON_VALIDATION.status(),
                Instant.now()
        );
        return ResponseEntity.status(CommonErrorCode.COMMON_VALIDATION.status()).body(body);
    }

    /**
     * 최후의 방어: 알 수 없는 예외.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception ex) {
        ErrorResponse body = new ErrorResponse(
                CommonErrorCode.COMMON_INTERNAL_ERROR.code(),
                CommonErrorCode.COMMON_INTERNAL_ERROR.defaultMessage(),
                CommonErrorCode.COMMON_INTERNAL_ERROR.status(),
                Instant.now()
        );
        return ResponseEntity.status(CommonErrorCode.COMMON_INTERNAL_ERROR.status()).body(body);
    }
}
