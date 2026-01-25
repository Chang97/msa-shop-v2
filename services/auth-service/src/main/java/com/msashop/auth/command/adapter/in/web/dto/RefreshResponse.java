package com.msashop.auth.command.adapter.in.web.dto;

public record RefreshResponse(
        String accessToken,
        String refreshToken
) { }
