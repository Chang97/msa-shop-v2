package com.msashop.common.web.exception;

public class ConflictException extends BusinessException {
    public ConflictException(ErrorCode errorCode) { super(errorCode); }
    public ConflictException(ErrorCode errorCode, String message) { super(errorCode, message); }
}
