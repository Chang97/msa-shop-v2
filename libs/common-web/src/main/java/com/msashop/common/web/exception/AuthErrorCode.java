package com.msashop.common.web.exception;

import com.msashop.common.web.exception.ErrorCode;

public enum AuthErrorCode implements ErrorCode {

    AUTH_INVALID_CREDENTIALS("AUTH_001", 401, "Invalid credentials."),
    AUTH_DISABLED_USER("AUTH_002", 401, "User is disabled"),
    AUTH_NOT_MATCHED_PASSWORD("AUTH_002", 401, "User Password not matched"),
    AUTH_REFRESH_MISSING("AUTH_010", 401, "Refresh token is missing."),
    AUTH_REFRESH_EXPIRED("AUTH_011", 401, "Refresh token expired."),
    AUTH_REFRESH_REVOKED("AUTH_012", 401, "Refresh token revoked."),
    AUTH_REFRESH_REPLAY("AUTH_013", 409, "Refresh token already used (replay suspected)."),
    AUTH_REFRESH_INVALID_FORMAT("AUTH_014", 401, "Invalid refresh token format");

    private final String code;
    private final int status;
    private final String defaultMessage;

    AuthErrorCode(String code, int status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    @Override public String code() { return code; }
    @Override public int status() { return status; }
    @Override public String defaultMessage() { return defaultMessage; }
}
