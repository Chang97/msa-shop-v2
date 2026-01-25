package com.msashop.auth.common.exception;

import com.msashop.auth.common.exception.BusinessException;

public class ConflictException extends BusinessException {
    public ConflictException(ErrorCode errorCode) { super(errorCode); }
    public ConflictException(ErrorCode errorCode, String message) { super(errorCode, message); }
}
