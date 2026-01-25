package com.msashop.auth.command.application.service.token;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 토큰 원문(raw)을 안전하게 저장하기 위해 해시로 변환하는 컴포넌트.
 *
 * 1차에서는 SHA-256(hex)을 사용:
 * - 저장 값 길이: 64 chars (hex)
 * - token_hash 컬럼 길이(512)에 충분히 들어감
 *
 * 참고:
 * - 운영에서는 HMAC(SHA-256 + 서버 시크릿)로 만들면 "DB 유출 시 오프라인 대입 공격"을 더 줄일 수 있음
 * - 로컬 단계에서는 SHA-256만으로도 설계 의도 전달 가능
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
