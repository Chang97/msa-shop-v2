package com.msashop.auth.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // ---- Auth ----
    AUTH_INVALID_CREDENTIALS("AUTH_001",HttpStatus.UNAUTHORIZED, "Invalid credentials."),
    AUTH_REFRESH_MISSING("AUTH_010", HttpStatus.UNAUTHORIZED, "Refresh token is missing."),
    AUTH_REFRESH_EXPIRED("AUTH_011", HttpStatus.UNAUTHORIZED, "Refresh token expired."),
    AUTH_REFRESH_REVOKED("AUTH_012", HttpStatus.UNAUTHORIZED, "Refresh token revoked."),
    AUTH_REFRESH_REPLAY("AUTH_013", HttpStatus.CONFLICT, "Refresh token already used (replay suspected)."),
    AUTH_REFRESH_INVALID_FORMAT("AUTH_014", HttpStatus.UNAUTHORIZED, "Invalid refresh token format"),

    // ---- Common ----
    COMMON_VALIDATION("COM_400", HttpStatus.BAD_REQUEST, "Validation failed."),
    COMMON_NOT_FOUND("COM_404", HttpStatus.NOT_FOUND, "Resource not found."),
    COMMON_UNAUTHORIZED("COM_401", HttpStatus.UNAUTHORIZED, "Unauthorized."),
    COMMON_CONFLICT("COM_409", HttpStatus.CONFLICT, "Conflict."),
    COMMON_INTERNAL_ERROR("COM_500", HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error.");

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public String code() { return code; }
    public HttpStatus status() { return status; }
    public String defaultMessage() { return defaultMessage; }
}
