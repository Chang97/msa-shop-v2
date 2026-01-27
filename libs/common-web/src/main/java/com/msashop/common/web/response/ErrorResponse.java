package com.msashop.common.web.response;

import java.time.Instant;

/**
 * 모든 서비스에서 동일한 에러 응답 스키마.
 */
public record ErrorResponse(
        String code,
        String message,
        int status,
        Instant timestamp
) {}
