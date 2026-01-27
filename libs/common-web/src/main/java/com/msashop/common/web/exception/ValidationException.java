package com.msashop.common.web.exception;

public class ValidationException extends BusinessException {
    public ValidationException(ErrorCode errorCode) { super(errorCode); }
    public ValidationException(ErrorCode errorCode, String message) { super(errorCode, message); }
}
