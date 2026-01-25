package com.msashop.auth.command.application.service.token;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Refresh Token 원문을 생성하는 컴포넌트.
 *
 * Refresh Token 설계(1차):
 * - raw token은 "tokenId.secret" 형태
 *   - tokenId: DB에서 토큰 레코드를 식별하기 위한 ID(유니크)
 *   - secret : 클라이언트만 알고 있어야 하는 랜덤 값(위변조/추측 방지)
 *
 * 저장 원칙:
 * - DB에는 raw token을 저장하지 않는다.
 * - DB에는 raw token의 hash(token_hash)만 저장한다.
 * - 클라이언트는 raw token을 보관하고, refresh 요청 시 raw token을 제출한다.
 *
 * 장점:
 * - DB가 유출돼도 raw refresh token 재사용이 어려움(해시만 있으므로)
 * - tokenId로 레코드 조회가 빠름(유니크 인덱스 활용)
 */
@Component
public class RefreshTokenGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    public GeneratedRefreshToken generate() {
        // tokenId: UUID 하이픈 제거(32 chars) -> DDL token_id varchar(64) 이내
        String tokenId = UUID.randomUUID().toString().replace("-", ""); // 32 chars

        // secret: 32바이트 랜덤 -> base64url(패딩 제거)로 문자열화
        // base64url을 쓰면 URL/JSON에서 안전하게 다룰 수 있음
        byte[] secretBytes = new byte[32];
        secureRandom.nextBytes(secretBytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);

        // 최종 raw refresh token (클라이언트에 내려줄 값)
        String raw = tokenId + "." + secret;

        return new GeneratedRefreshToken(tokenId, raw);
    }

    public record GeneratedRefreshToken(String tokenId, String rawToken) {}
}
