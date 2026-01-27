package com.msashop.common.web.exception;

public class NotFoundException extends BusinessException {
    public NotFoundException(ErrorCode errorCode) { super(errorCode); }
    public NotFoundException(ErrorCode errorCode, String message) { super(errorCode, message); }
}
