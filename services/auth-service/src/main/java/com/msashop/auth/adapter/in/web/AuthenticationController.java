package com.msashop.auth.adapter.in.web;

import com.msashop.auth.adapter.in.web.cookie.RefreshCookieFactory;
import com.msashop.auth.adapter.in.web.dto.LoginRequest;
import com.msashop.auth.adapter.in.web.dto.LoginResponse;
import com.msashop.auth.adapter.in.web.mapper.AuthWebMapper;
import com.msashop.auth.application.port.in.LoginUseCase;
import com.msashop.auth.application.port.in.LogoutUseCase;
import com.msashop.auth.application.port.in.RefreshUseCase;
import com.msashop.auth.application.port.in.model.LoginResult;
import com.msashop.auth.application.port.in.model.LogoutCommand;
import com.msashop.auth.application.port.in.model.RefreshCommand;
import com.msashop.common.web.exception.AuthErrorCode;
import com.msashop.common.web.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 로그인, 토큰 재발급, 로그아웃 같은 인증 API를 제공하는 컨트롤러다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private static final String REFRESH_COOKIE_NAME = "rt";

    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RefreshUseCase refreshUseCase;
    private final RefreshCookieFactory refreshCookieFactory;

    /**
     * 요청이 HTTPS인지 확인해 보안 쿠키 적용 여부를 결정한다.
     */
    private boolean isSecureCookie(HttpServletRequest request) {
        return request.isSecure();
    }

    /**
     * 로그인 성공 시 access token은 응답 본문으로, refresh token은 HttpOnly 쿠키로 내려준다.
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
     * 쿠키의 refresh token으로 새 access token을 발급하고 refresh token 쿠키도 회전한다.
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletRequest request
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(AuthErrorCode.AUTH_REFRESH_MISSING);
        }

        var result = refreshUseCase.refresh(new RefreshCommand(refreshToken));
        boolean secure = isSecureCookie(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.create(result.refreshToken(), secure).toString())
                .body(Map.of("accessToken", result.accessToken()));
    }

    /**
     * refresh token을 무효화하고 브라우저의 refresh token 쿠키를 즉시 만료시킨다.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletRequest request
    ) {
        boolean secure = isSecureCookie(request);

        if (refreshToken != null && !refreshToken.isBlank()) {
            logoutUseCase.logout(new LogoutCommand(refreshToken));
        }

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.delete(secure).toString())
                .build();
    }
}
