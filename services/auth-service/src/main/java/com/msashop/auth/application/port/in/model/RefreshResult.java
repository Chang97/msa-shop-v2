package com.msashop.auth.application.port.in.model;

public record RefreshResult(
        String accessToken,
        String refreshToken
) {
}

