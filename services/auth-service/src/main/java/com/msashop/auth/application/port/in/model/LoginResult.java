package com.msashop.auth.application.port.in.model;

public record LoginResult(
        String accessToken,
        String refreshToken
) {
}

