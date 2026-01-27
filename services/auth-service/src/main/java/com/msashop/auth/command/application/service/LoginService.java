package com.msashop.auth.command.application.service;

import com.msashop.auth.command.application.port.in.LoginUseCase;
import com.msashop.auth.command.application.port.in.model.LoginCommand;
import com.msashop.auth.command.application.port.in.model.LoginResult;
import com.msashop.auth.command.application.port.out.LoadUserPort;
import com.msashop.auth.command.application.port.out.LoadUserPort.AuthUserRecord;
import com.msashop.auth.command.application.port.out.RefreshTokenPort;
import com.msashop.auth.command.application.service.token.RefreshTokenGenerator;
import com.msashop.auth.command.application.service.token.TokenHasher;
import com.msashop.auth.config.auth.RefreshTokenProperties;
import com.msashop.auth.config.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 로그인 유스케이스 구현체.
 *
 * 책임:
 * 1) 사용자 조회
 * 2) 비밀번호 검증(Argon2)
 * 3) Access Token 발급
 * 4) Refresh Token 발급 + DB 저장(해시만)
 *
 * 참고:
 * - Refresh rotate / logout revoke는 별도 유스케이스로 분리(RefreshService/LogoutService)
 */
@Service
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {
    private final LoadUserPort loadUserPort;
    private final PasswordEncoder passwordEncoder;

    private final RefreshTokenPort refreshTokenPort;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final TokenHasher tokenHasher;
    private final RefreshTokenProperties refreshProps;

    @Transactional
    @Override
    public LoginResult login(LoginCommand command) {
        // 1) 사용자 조회
        AuthUserRecord user = loadUserPort.findByLoginId(command.loginId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        // 2) 활성 사용자 여부(use_yn) 체크
        if (Boolean.FALSE.equals(user.useYn())) {
            throw new IllegalArgumentException("User is disabled");
        }

        // 3) 비밀번호 검증(Argon2 해시 비교)
        boolean matches = passwordEncoder.matches(command.password(), user.passwordHash());
        if (!matches) {
            // TODO: 실패 카운트 증가(user_password_fail_cnt) 등은 다음 단계에서 트랜잭션으로
            throw new IllegalArgumentException("Invalid credentials");
        }

        // 4) Access Token 발급 (roles는 1차는 고정, 이후 user_role_map 조인으로 확장)
        String accessToken = issueDevToken(String.valueOf(user.userId()), List.of("ROLE_ADMIN"));

        // 5) Refresh Token 원문 생성(클라이언트 전달용)
        var generated = refreshTokenGenerator.generate();

        // 6) DB 저장용(NewRefreshToken) 조립: hash 계산 + 만료(expiresAt) 산출
        String refreshRaw = generated.rawToken();
        String refreshHash = tokenHasher.sha256Hex(refreshRaw);
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(refreshProps.ttlSeconds());

        // 7) DB 저장: raw는 저장하지 않고, hash만 저장
        refreshTokenPort.save(new RefreshTokenPort.NewRefreshToken(
                generated.tokenId(),
                refreshHash,
                user.userId(),
                expiresAt
        ));
        // 8) 응답: access + refresh(raw)
        return new LoginResult(accessToken, refreshRaw);
    }

    private final JwtEncoder jwtEncoder;
    private final JwtProperties props;

    /**
     * 개발용 토큰 발급.
     * - userId / roles를 고정값으로 발급해서 Gateway 검증 파이프라인이 정상인지 확인
     */
    private String issueDevToken(String userId, List<String> roles) {
        Instant now = Instant.now();

        /*
         * JWT Claims:
         * - issuer(iss): 토큰 발급자
         * - issuedAt(iat): 발급 시각
         * - expiresAt(exp): 만료 시각
         * - subject(sub): 보통 사용자 식별자
         * - roles: 커스텀 클레임(서비스 권한 판단용)
         */
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(props.accessTokenTtlSeconds()))
                .subject(userId)
                .claim("roles", roles)
                .build();

        /*
         * JWS Header:
         * - alg: 서명 알고리즘(RS256)
         * - (추후) kid: 키가 여러 개일 때 어떤 키로 서명했는지 식별하기 위한 key id
         */
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
