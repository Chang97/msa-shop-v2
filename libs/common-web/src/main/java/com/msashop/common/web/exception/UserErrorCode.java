package com.msashop.common.web.exception;

public enum UserErrorCode implements ErrorCode {

    USER_CURRENT_MISSING("USER_001", 401, "Unauthenticated request: CurrentUser is missing."),
    USER_NOT_FOUND("USER_002", 401, "User Not Found.");

    private final String code;
    private final int status;
    private final String defaultMessage;

    UserErrorCode(String code, int status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    @Override public String code() { return code; }
    @Override public int status() { return status; }
    @Override public String defaultMessage() { return defaultMessage; }
}
