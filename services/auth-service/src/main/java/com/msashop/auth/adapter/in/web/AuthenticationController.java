package com.msashop.auth.adapter.in.web;

import com.msashop.auth.adapter.in.web.cookie.RefreshCookieFactory;
import com.msashop.auth.adapter.in.web.dto.*;
import com.msashop.auth.adapter.in.web.mapper.AuthWebMapper;
import com.msashop.auth.application.port.in.LoginUseCase;
import com.msashop.auth.application.port.in.LogoutUseCase;
import com.msashop.auth.application.port.in.RefreshUseCase;
import com.msashop.auth.application.port.in.model.LoginResult;
import com.msashop.auth.application.port.in.model.LogoutCommand;
import com.msashop.auth.application.port.in.model.RefreshCommand;
import com.msashop.common.web.exception.AuthErrorCode;
import com.msashop.common.web.exception.UnauthorizedException;
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
     * 로컬(http)?�서??Secure=false가 ?�요.
     * ?�영(https)?�서??true�?고정?�야 ??
     */
    private boolean isSecureCookie(HttpServletRequest request) {
        return request.isSecure(); // https�?true
    }

    /**
     * 로그??
     * - accessToken?� JSON 바디�??�답
     * - refreshToken?� HttpOnly Cookie�??�??
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
     * ?�큰 ?�발�?rotate):
     * - refreshToken?� 쿠키?�서 받는??@CookieValue)
     * - ?�공 ???�로??refreshToken?�로 쿠키�?교체(rotate)
     * - accessToken?� JSON 바디�??�답
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletRequest request
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException(AuthErrorCode.AUTH_REFRESH_MISSING);

        }

        var result = refreshUseCase.refresh(new RefreshCommand(refreshToken));
        boolean secure = isSecureCookie(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.create(result.refreshToken(), secure).toString())
                .body(Map.of("accessToken", result.accessToken()));
    }

    /**
     * 로그?�웃:
     * - refreshToken??revoke?�고
     * - refresh cookie�???��?�다
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletRequest request
    ) {
        boolean secure = isSecureCookie(request);

        if (refreshToken != null && !refreshToken.isBlank()) {
            // 로그?�웃?� 멱등 처리: ?��??�서 ?�큰 ?�음/만료/revoked?�도 ?�외�?굳이 ?�리지 ?�도�??�계
            logoutUseCase.logout(new LogoutCommand(refreshToken));
        }


        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.delete(secure).toString())
                .build();
    }
}

