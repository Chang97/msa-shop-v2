package com.msashop.auth.application.service;

import com.msashop.auth.application.port.in.LoginUseCase;
import com.msashop.auth.application.port.in.model.LoginCommand;
import com.msashop.auth.application.port.in.model.LoginResult;
import com.msashop.auth.application.port.out.LoadUserPort;
import com.msashop.auth.application.port.out.LoadUserPort.AuthUserRecord;
import com.msashop.auth.application.port.out.RefreshTokenPort;
import com.msashop.auth.application.service.token.RefreshTokenGenerator;
import com.msashop.auth.application.service.token.TokenHasher;
import com.msashop.auth.application.service.token.TokenIssuer;
import com.msashop.auth.config.auth.RefreshTokenProperties;
import com.msashop.common.web.exception.AuthErrorCode;
import com.msashop.common.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 로그인 요청을 처리하고 access token, refresh token을 발급하는 서비스다.
 *
 * 처리 흐름:
 * 1. 로그인 아이디로 사용자를 조회한다.
 * 2. 사용자 활성 여부와 비밀번호를 검증한다.
 * 3. access token을 발급한다.
 * 4. refresh token을 생성하고 해시값을 저장한다.
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
        // 로그인 아이디로 인증 대상 사용자를 조회한다.
        AuthUserRecord user = loadUserPort.findByLoginId(command.loginId())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_INVALID_CREDENTIALS));

        // 비활성 사용자면 로그인할 수 없다.
        if (Boolean.FALSE.equals(user.enabled())) {
            throw new BusinessException(AuthErrorCode.AUTH_DISABLED_USER);
        }

        // 입력한 비밀번호와 저장된 비밀번호 해시가 일치하는지 검증한다.
        boolean matches = passwordEncoder.matches(command.password(), user.passwordHash());
        if (!matches) {
            // TODO: 추후 로그인 실패 횟수 누적과 계정 잠금 정책을 붙일 수 있다.
            throw new BusinessException(AuthErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        // 사용자 권한 목록을 access token claim에 담는다.
        List<String> roles = user.roles() == null ? List.of() : user.roles();
        String accessToken = tokenIssuer.issueAccessToken(user.userId(), roles);

        // refresh token 원문과 저장용 해시를 만든다.
        var generated = refreshTokenGenerator.generate();
        String refreshRaw = generated.rawToken();
        String refreshHash = tokenHasher.sha256Hex(refreshRaw);
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(refreshProps.ttlSeconds());

        // DB에는 refresh token 원문이 아니라 해시와 만료 시각만 저장한다.
        refreshTokenPort.save(new RefreshTokenPort.NewRefreshToken(
                refreshHash,
                user.userId(),
                expiresAt
        ));

        return new LoginResult(accessToken, refreshRaw);
    }
}
