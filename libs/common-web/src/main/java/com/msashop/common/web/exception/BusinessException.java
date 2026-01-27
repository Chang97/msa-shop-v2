package com.msashop.common.web.exception;

/**
 * 모든 비즈니스 예외의 공통 베이스.
 * - 특정 서비스 enum에 결합되지 않도록 ErrorCode(interface)만 참조한다.
 */
public abstract class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    protected BusinessException(ErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
    }

    protected BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
