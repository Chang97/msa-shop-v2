package com.msashop.auth.application.service.token;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * refresh token 원문을 저장용 SHA-256 해시 문자열로 변환하는 컴포넌트다.
 *
 * DB에는 refresh token 원문을 직접 저장하지 않고 해시값만 저장해 유출 시 영향을 줄인다.
 */
@Component
public class TokenHasher {

    private static final HexFormat HEX = HexFormat.of();

    public String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(digest);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to hash token", e);
        }
    }
}
