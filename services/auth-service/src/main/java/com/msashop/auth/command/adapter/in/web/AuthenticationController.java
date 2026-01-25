package com.msashop.auth.command.adapter.in.web;

import com.msashop.auth.command.adapter.in.web.cookie.RefreshCookieFactory;
import com.msashop.auth.command.adapter.in.web.dto.*;
import com.msashop.auth.command.adapter.in.web.mapper.AuthWebMapper;
import com.msashop.auth.command.application.port.in.LoginUseCase;
import com.msashop.auth.command.application.port.in.LogoutUseCase;
import com.msashop.auth.command.application.port.in.RefreshUseCase;
import com.msashop.auth.command.application.port.in.model.LoginResult;
import com.msashop.auth.command.application.port.in.model.LogoutCommand;
import com.msashop.auth.command.application.port.in.model.RefreshCommand;
import com.msashop.auth.common.exception.ErrorCode;
import com.msashop.auth.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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
    private final RefreshCookieFactory refreshCookieFactory;

    /**
     * 로컬(http)에서는 Secure=false가 필요.
     * 운영(https)에서는 true로 고정해야 함.
     */
    private boolean isSecureCookie(HttpServletRequest request) {
        return request.isSecure(); // https면 true
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
                .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.create(result.refreshToken(), secure).toString())
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
            throw new UnauthorizedException(ErrorCode.AUTH_REFRESH_MISSING);
        }

        var result = refreshUseCase.refresh(new RefreshCommand(refreshToken));
        boolean secure = isSecureCookie(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.create(result.refreshToken(), secure).toString())
                .body(Map.of("accessToken", result.accessToken()));
    }

    /**
     * 로그아웃:
     * - refreshToken을 revoke하고
     * - refresh cookie를 삭제한다
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletRequest request
    ) {
        boolean secure = isSecureCookie(request);

        if (refreshToken != null && !refreshToken.isBlank()) {
            // 로그아웃은 멱등 처리: 내부에서 토큰 없음/만료/revoked여도 예외를 굳이 올리지 않도록 설계
            logoutUseCase.logout(new LogoutCommand(refreshToken));
        }


        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.delete(secure).toString())
                .build();
    }
}
