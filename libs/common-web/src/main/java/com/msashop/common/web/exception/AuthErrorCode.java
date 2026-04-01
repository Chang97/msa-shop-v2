package com.msashop.common.web.exception;

public enum AuthErrorCode implements ErrorCode {

    AUTH_INVALID_CREDENTIALS("AUTH_001", 401, "아이디 또는 비밀번호가 올바르지 않습니다."),
    AUTH_DISABLED_USER("AUTH_002", 401, "비활성화된 사용자입니다."),
    AUTH_NOT_MATCHED_PASSWORD("AUTH_003", 401, "비밀번호가 일치하지 않습니다."),
    AUTH_LOGIN_LOCKED("AUTH_004", 423, "로그인 실패 횟수 초과로 계정이 일시 잠금되었습니다."),
    AUTH_REFRESH_MISSING("AUTH_010", 401, "리프레시 토큰이 없습니다."),
    AUTH_REFRESH_EXPIRED("AUTH_011", 401, "리프레시 토큰이 만료되었습니다."),
    AUTH_REFRESH_REVOKED("AUTH_012", 401, "리프레시 토큰이 폐기되었습니다."),
    AUTH_REFRESH_REPLAY("AUTH_013", 409, "이미 사용된 리프레시 토큰입니다."),
    AUTH_REFRESH_INVALID_FORMAT("AUTH_014", 401, "리프레시 토큰 형식이 올바르지 않습니다."),
    AUTH_REFRESH_INVALID("AUTH_015", 401, "유효하지 않은 리프레시 토큰입니다.");

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
