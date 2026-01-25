package com.msashop.auth.common.response;

import java.time.Instant;
import java.time.LocalDateTime;

public record ErrorResponse(
        String code,
        String message,
        int status,
        Instant timestamp
) {}