package com.msashop.auth.application.service.token;

import com.msashop.auth.config.jwt.JwtProperties;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

/**
 * RS256 湲곕컲 JWT Access Token 諛쒓툒 援ы쁽泥?
 *
 * ?숈옉:
 * - JwtClaimsSet ?앹꽦 (iss, iat, exp, sub, roles)
 * - JwsHeader(alg=RS256) 吏??
 * - JwtEncoder(Nimbus)濡??쒕챸?섏뿬 JWT 臾몄옄???앹꽦
 *
 * 二쇱쓽:
 * - ???대옒?ㅻ뒗 "諛쒓툒"留??대떦 (寃利앹? Gateway ResourceServer媛 ?대떦)
 * - 誘쇨컧?뺣낫(?대찓???대쫫 ?????좏겙???ｌ? ?딆쓬
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

