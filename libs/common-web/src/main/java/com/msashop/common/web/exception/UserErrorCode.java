package com.msashop.common.web.exception;

public enum UserErrorCode implements ErrorCode {

    USER_CURRENT_MISSING("USER_001", 404, "인증 사용자 정보를 찾을 수 없습니다."),
    USER_NOT_FOUND("USER_002", 404, "사용자를 찾을 수 없습니다.");

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
