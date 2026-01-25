package com.msashop.auth.command.adapter.in.web;

import com.msashop.auth.command.adapter.in.web.dto.*;
import com.msashop.auth.command.adapter.in.web.mapper.AuthWebMapper;
import com.msashop.auth.command.application.port.in.LoginUseCase;
import com.msashop.auth.command.application.port.in.LogoutUseCase;
import com.msashop.auth.command.application.port.in.RefreshUseCase;
import com.msashop.auth.command.application.port.in.model.LoginResult;
import com.msashop.auth.command.application.port.in.model.LogoutCommand;
import com.msashop.auth.command.application.port.in.model.RefreshCommand;
import com.msashop.auth.config.auth.RefreshTokenProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private static final String REFRESH_COOKIE_NAME = "rt";
    private static final String COOKIE_PATH = "/api/auth";

    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RefreshUseCase refreshUseCase;
    private final RefreshTokenProperties refreshTokenProperties;

    /**
     * 로컬(http)에서는 Secure=false가 필요.
     * 운영(https)에서는 true로 고정해야 함.
     */
    private boolean isSecureCookie(HttpServletRequest request) {
        return request.isSecure(); // https면 true
    }

    private ResponseCookie refreshCookie(String refreshToken, boolean secure) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(refreshTokenProperties.ttlSeconds())
                .build();
    }

    private ResponseCookie deleteRefreshCookie(boolean secure) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
    }

    /**
     * 로그인:
     * - accessToken은 JSON 바디로 응답
     * - refreshToken은 HttpOnly Cookie로 저장
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest request
    ) {
        LoginResult result = loginUseCase.login(AuthWebMapper.toCommand(req));
        boolean secure = isSecureCookie(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken(), secure).toString())
                .body(new LoginResponse(result.accessToken()));
    }

    /**
     * 토큰 재발급(rotate):
     * - refreshToken은 쿠키에서 받는다(@CookieValue)
     * - 성공 시 새로운 refreshToken으로 쿠키를 교체(rotate)
     * - accessToken은 JSON 바디로 응답
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletRequest request
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401).body(Map.of("message", "Refresh token is missing"));
        }

        var result = refreshUseCase.refresh(new RefreshCommand(refreshToken));
        boolean secure = isSecureCookie(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken(), secure).toString())
                .body(Map.of("accessToken", result.accessToken()));
    }

    /**
     * 로그아웃:
     * - refreshToken을 revoke하고
     * - refresh cookie를 삭제한다
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletRequest request
    ) {
        boolean secure = isSecureCookie(request);

        if (refreshToken != null && !refreshToken.isBlank()) {
            logoutUseCase.logout(new LogoutCommand(refreshToken));
        }

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, deleteRefreshCookie(secure).toString())
                .build();
    }
}
