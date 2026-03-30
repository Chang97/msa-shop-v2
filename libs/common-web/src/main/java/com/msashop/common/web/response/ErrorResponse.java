package com.msashop.common.web.response;

import java.time.Instant;
import java.util.List;

/**
 * 모든 서비스에서 공통으로 사용하는 에러 응답 스키마.
 *
 * @param code 에러 코드
 * @param message 사용자에게 전달할 에러 메시지
 * @param status HTTP 상태 코드
 * @param timestamp 에러 응답 생성 시각
 * @param path 에러가 발생한 요청 경로
 * @param traceId 서버 로그 추적용 식별자
 * @param fieldErrors 입력값 검증 실패 상세 목록
 */
public record ErrorResponse(
        String code,
        String message,
        int status,
        Instant timestamp,
        String path,
        String traceId,
        List<FieldErrorDetail> fieldErrors
) {
    /**
     * 입력값 검증 실패 상세 정보.
     *
     * @param field 검증에 실패한 필드명
     * @param message 해당 필드의 에러 메시지
     */
    public record FieldErrorDetail(
            String field,
            String message
    ) {
    }
}
