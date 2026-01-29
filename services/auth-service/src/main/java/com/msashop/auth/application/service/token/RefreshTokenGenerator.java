package com.msashop.auth.application.service.token;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Refresh Token 생성 (랜덤 문자열 하나만 사용).
 * DB에는 hash(token_hash)만 저장한다.
 */
@Component
public class RefreshTokenGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    public GeneratedRefreshToken generate() {
        byte[] secretBytes = new byte[48];
        secureRandom.nextBytes(secretBytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);

        return new GeneratedRefreshToken(raw);
    }

    public record GeneratedRefreshToken(String rawToken) {}
}

