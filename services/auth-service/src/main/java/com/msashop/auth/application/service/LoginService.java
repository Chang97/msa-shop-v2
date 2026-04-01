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
 * 1. loginId 기준 잠금 상태를 먼저 확인한다.
 * 2. 사용자 계정을 조회한다.
 * 3. 비활성 계정인지 확인한다.
 * 4. 비밀번호를 검증한다.
 * 5. 실패 시 loginId 기준으로 실패 횟수를 누적한다.
 * 6. 임계치에 도달하면 잠금 응답을 반환한다.
 * 7. 성공 시 실패 횟수를 비우고 access token, refresh token을 발급한다.
 *
 * 잠금 정책:
 * - 존재 여부와 무관하게 loginId 기준으로 실패 횟수를 누적한다.
 * - 이렇게 하면 구현이 단순하고, 특정 계정만 별도로 추적하는 추가 분기가 필요 없다.
 */
@Service
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {
    private final LoadUserPort loadUserPort;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptLockService loginAttemptLockService;
    private final RefreshTokenPort refreshTokenPort;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final TokenHasher tokenHasher;
    private final RefreshTokenProperties refreshProps;
    private final TokenIssuer tokenIssuer;

    @Transactional
    @Override
    public LoginResult login(LoginCommand command) {
        if (loginAttemptLockService.isLocked(command.loginId())) {
            throw new BusinessException(AuthErrorCode.AUTH_LOGIN_LOCKED);
        }

        AuthUserRecord user = loadUserPort.findByLoginId(command.loginId())
                .orElseThrow(() -> invalidCredentials(command.loginId()));

        if (Boolean.FALSE.equals(user.enabled())) {
            throw new BusinessException(AuthErrorCode.AUTH_DISABLED_USER);
        }

        boolean matches = passwordEncoder.matches(command.password(), user.passwordHash());
        if (!matches) {
            throw invalidCredentials(command.loginId());
        }

        // 로그인 성공 시 남아 있던 실패 횟수를 지워 이후 정상 로그인에 영향이 없게 한다.
        loginAttemptLockService.clearFailures(command.loginId());

        List<String> roles = user.roles() == null ? List.of() : user.roles();
        String accessToken = tokenIssuer.issueAccessToken(user.userId(), roles);

        var generated = refreshTokenGenerator.generate();
        String refreshRaw = generated.rawToken();
        String refreshHash = tokenHasher.sha256Hex(refreshRaw);
        Instant expiresAt = Instant.now().plusSeconds(refreshProps.ttlSeconds());

        refreshTokenPort.save(new RefreshTokenPort.NewRefreshToken(
                refreshHash,
                user.userId(),
                expiresAt
        ));

        return new LoginResult(accessToken, refreshRaw);
    }

    private BusinessException invalidCredentials(String loginId) {
        boolean locked = loginAttemptLockService.recordFailure(loginId);
        if (locked) {
            return new BusinessException(AuthErrorCode.AUTH_LOGIN_LOCKED);
        }
        return new BusinessException(AuthErrorCode.AUTH_INVALID_CREDENTIALS);
    }
}
