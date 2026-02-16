package com.msashop.common.web.exception;

/**
 * 모든 서비스에서 공통으로 쓰는 에러 코드.
 * - 서비스별 코드(enum)와 분리한다.
 */
public enum CommonErrorCode implements ErrorCode {

    COMMON_VALIDATION("COM_400", 400, "Validation failed."),
    COMMON_UNAUTHORIZED("COM_401", 401, "Unauthorized."),
    COMMON_FORBIDDEN("COM_403", 403, "Forbidden."),
    COMMON_NOT_FOUND("COM_404", 404, "Resource not found."),
    COMMON_CONFLICT("COM_409", 409, "Conflict."),
    COMMON_INTERNAL_ERROR("COM_500", 500, "Internal server error.");

    private final String code;
    private final int status;
    private final String defaultMessage;

    CommonErrorCode(String code, int status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    @Override public String code() { return code; }
    @Override public int status() { return status; }
    @Override public String defaultMessage() { return defaultMessage; }
}
