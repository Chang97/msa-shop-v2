package com.msashop.auth.adapter.in.web;

import com.msashop.auth.adapter.in.web.dto.AuthMeResponse;
import com.msashop.auth.application.port.in.GetAuthMeUseCase;
import com.msashop.auth.common.security.CurrentUser;
import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthQueryController {

    private final GetAuthMeUseCase getAuthMeUseCase;

    @GetMapping("/me")
    public ResponseEntity<AuthMeResponse> me(@AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null || currentUser.userId() == null) {
            throw new BusinessException(CommonErrorCode.COMMON_UNAUTHORIZED);
        }

        return ResponseEntity.ok(getAuthMeUseCase.getMe(currentUser.userId()));
    }
}
