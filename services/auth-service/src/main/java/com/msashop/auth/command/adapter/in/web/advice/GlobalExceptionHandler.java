package com.msashop.auth.command.adapter.in.web.advice;

import com.msashop.auth.common.exception.*;
import com.msashop.auth.common.response.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode ec = e.errorCode();
        return ResponseEntity.status(ec.status())
                .body(new ErrorResponse(ec.code(), e.getMessage(), ec.status().value(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ErrorCode.COMMON_VALIDATION.code(),
                        ErrorCode.COMMON_VALIDATION.defaultMessage(),
                        400, Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e) {
        ErrorCode ec = ErrorCode.COMMON_INTERNAL_ERROR;
        return ResponseEntity.status(ec.status())
                .body(new ErrorResponse(ec.code(), ec.defaultMessage(), 500, Instant.now()));
    }

}
