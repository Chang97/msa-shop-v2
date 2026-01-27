package com.msashop.common.web.exception;

/**
 * 서비스별 ErrorCode enum이 구현할 최소 계약.
 *
 * - 공용 모듈은 "어떤 서비스의 코드인지" 모른다.
 * - 그래서 enum 대신 interface로 추상화한다.
 */
public interface ErrorCode {
    String code();            // 예: AUTH_001, USER_010, COM_500
    int status();             // HTTP status code (예: 400, 401, 404, 409, 500)
    String defaultMessage();  // 기본 메시지
}
