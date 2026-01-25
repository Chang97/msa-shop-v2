package com.msashop.auth.command.application.port.in.model;

public record LoginResult(
        String accessToken,
        String refreshToken
) {
}
