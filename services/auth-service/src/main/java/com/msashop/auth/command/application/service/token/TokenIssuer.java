package com.msashop.auth.command.application.service.token;

import com.msashop.auth.config.jwt.JwtProperties;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

/**
 * RS256 기반 JWT Access Token 발급 구현체.
 *
 * 동작:
 * - JwtClaimsSet 생성 (iss, iat, exp, sub, roles)
 * - JwsHeader(alg=RS256) 지정
 * - JwtEncoder(Nimbus)로 서명하여 JWT 문자열 생성
 *
 * 주의:
 * - 이 클래스는 "발급"만 담당 (검증은 Gateway ResourceServer가 담당)
 * - 민감정보(이메일/이름 등)는 토큰에 넣지 않음
 */
@Component
@RequiredArgsConstructor
public class TokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties props;

    public String issueAccessToken(Long userId, List<String> roles) {
        Instant now = Instant.now();

        List<String> safeRoles = (roles == null) ? List.of() : List.copyOf(roles);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(props.accessTokenTtlSeconds()))
                .subject(String.valueOf(userId))
                .claim("roles", safeRoles)
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();

        return jwtEncoder
                .encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }
}