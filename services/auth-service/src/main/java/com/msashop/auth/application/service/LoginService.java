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
import com.msashop.auth.config.jwt.JwtProperties;
import com.msashop.common.web.exception.AuthErrorCode;
import com.msashop.common.web.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;

/**
 * 濡쒓렇???좎뒪耳?댁뒪 援ы쁽泥?
 *
 * 梨낆엫:
 * 1) ?ъ슜??議고쉶
 * 2) 鍮꾨?踰덊샇 寃利?Argon2)
 * 3) Access Token 諛쒓툒
 * 4) Refresh Token 諛쒓툒 + DB ????댁떆留?
 *
 * 李멸퀬:
 * - Refresh rotate / logout revoke??蹂꾨룄 ?좎뒪耳?댁뒪濡?遺꾨━(RefreshService/LogoutService)
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
        // 1) ?ъ슜??議고쉶
        AuthUserRecord user = loadUserPort.findByLoginId(command.loginId())
                .orElseThrow(() -> new UnauthorizedException(AuthErrorCode.AUTH_INVALID_CREDENTIALS));
        // 2) ?쒖꽦 ?ъ슜???щ?(use_yn) 泥댄겕
        if (Boolean.FALSE.equals(user.enabled())) {
            throw new UnauthorizedException(AuthErrorCode.AUTH_DISABLED_USER);
        }

        // 3) 鍮꾨?踰덊샇 寃利?Argon2 ?댁떆 鍮꾧탳)
        boolean matches = passwordEncoder.matches(command.password(), user.passwordHash());
        if (!matches) {
            // TODO: ?ㅽ뙣 移댁슫??利앷?(user_password_fail_cnt) ?깆? ?ㅼ쓬 ?④퀎?먯꽌 ?몃옖??뀡?쇰줈
            throw new UnauthorizedException(AuthErrorCode.AUTH_NOT_MATCHED_PASSWORD);
        }

        // 4) Access Token 諛쒓툒 (roles??1李⑤뒗 怨좎젙, ?댄썑 user_role_map 議곗씤?쇰줈 ?뺤옣)
        String accessToken = tokenIssuer.issueAccessToken(user.userId(), List.of("ROLE_USER"));

        // 5) Refresh Token ?먮Ц ?앹꽦(?대씪?댁뼵???꾨떖??
        var generated = refreshTokenGenerator.generate();

        // 6) DB ??μ슜(NewRefreshToken) 議곕┰: hash 怨꾩궛 + 留뚮즺(expiresAt) ?곗텧
        String refreshRaw = generated.rawToken();
        String refreshHash = tokenHasher.sha256Hex(refreshRaw);
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(refreshProps.ttlSeconds());

        // 7) DB ??? raw????ν븯吏 ?딄퀬, hash留????
        refreshTokenPort.save(new RefreshTokenPort.NewRefreshToken(
                refreshHash,
                user.userId(),
                expiresAt
        ));
        // 8) ?묐떟: access + refresh(raw)
        return new LoginResult(accessToken, refreshRaw);
    }

    private final JwtEncoder jwtEncoder;
    private final JwtProperties props;
}

