package com.msashop.auth.adapter.in.web.dto;

public record RefreshResponse(
        String accessToken,
        String refreshToken
) { }

