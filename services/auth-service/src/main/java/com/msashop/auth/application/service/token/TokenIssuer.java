package com.msashop.auth.application.service.token;

import com.msashop.auth.config.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * RS256 서명 방식으로 JWT access token을 발급하는 컴포넌트다.
 *
 * access token에는 issuer, 발급 시각, 만료 시각, 사용자 식별자, 권한 목록을 담는다.
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
