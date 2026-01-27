package com.msashop.auth.command.application.service;

import com.msashop.auth.command.application.port.in.LoginUseCase;
import com.msashop.auth.command.application.port.in.model.LoginCommand;
import com.msashop.auth.command.application.port.in.model.LoginResult;
import com.msashop.auth.command.application.port.out.LoadUserPort;
import com.msashop.auth.command.application.port.out.LoadUserPort.AuthUserRecord;
import com.msashop.auth.command.application.port.out.RefreshTokenPort;
import com.msashop.auth.command.application.service.token.RefreshTokenGenerator;
import com.msashop.auth.command.application.service.token.TokenHasher;
import com.msashop.auth.command.application.service.token.TokenIssuer;
import com.msashop.auth.config.auth.RefreshTokenProperties;
import com.msashop.auth.config.jwt.JwtProperties;
import com.msashop.common.web.exception.AuthErrorCode;
import com.msashop.common.web.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
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
    private final TokenIssuer tokenIssuer;

    @Transactional
    @Override
    public LoginResult login(LoginCommand command) {
        // 1) 사용자 조회
        AuthUserRecord user = loadUserPort.findByLoginId(command.loginId())
                .orElseThrow(() -> new UnauthorizedException(AuthErrorCode.AUTH_INVALID_CREDENTIALS));
        // 2) 활성 사용자 여부(use_yn) 체크
        if (Boolean.FALSE.equals(user.useYn())) {
            throw new UnauthorizedException(AuthErrorCode.AUTH_DISABLED_USER);
        }

        // 3) 비밀번호 검증(Argon2 해시 비교)
        boolean matches = passwordEncoder.matches(command.password(), user.passwordHash());
        if (!matches) {
            // TODO: 실패 카운트 증가(user_password_fail_cnt) 등은 다음 단계에서 트랜잭션으로
            throw new UnauthorizedException(AuthErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        // 4) Access Token 발급 (roles는 1차는 고정, 이후 user_role_map 조인으로 확장)
        String accessToken = tokenIssuer.issueAccessToken(user.userId(), List.of("ROLE_USER"));

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
}
