package com.msashop.common.web.exception;

/**
 * 모든 서비스에서 공통으로 쓰는 에러 코드.
 * - 서비스별 코드(enum)와 분리한다.
 */
public enum CommonErrorCode implements ErrorCode {

    COMMON_VALIDATION("COM_400", 400, "입력값 검증에 실패했습니다."),
    COMMON_UNAUTHORIZED("COM_401", 401, "인증이 필요합니다."),
    COMMON_FORBIDDEN("COM_403", 403, "접근 권한이 없습니다."),
    COMMON_NOT_FOUND("COM_404", 404, "요청한 대상을 찾을 수 없습니다."),
    COMMON_CONFLICT("COM_409", 409, "요청을 처리할 수 없는 상태입니다."),
    COMMON_INTERNAL_ERROR("COM_500", 500, "서버 내부 오류가 발생했습니다.");

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
