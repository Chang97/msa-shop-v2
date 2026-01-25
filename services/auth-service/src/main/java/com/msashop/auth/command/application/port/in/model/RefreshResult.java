package com.msashop.auth.command.application.port.in.model;

public record RefreshResult(
        String accessToken,
        String refreshToken
) {
}
