package com.msashop.user.command.adapter.in.web;

import com.msashop.user.command.application.port.in.DeactivateMeUseCase;
import com.msashop.user.common.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User Command API (CUD).
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserCommandController {

    private final DeactivateMeUseCase deactivateMeUseCase;

    /**
     * 내 계정 비활성화.
     * - gateway에서 내려준 X-User-Id 기반으로 CurrentUser가 세팅되어야 한다.
     */
    @PatchMapping("/me/deactivate")
    public ResponseEntity<Void> deactivateMe(@AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null || currentUser.userId() == null) {
            // 공용 예외 모듈을 쓰면 UnauthorizedException 던지는게 더 깔끔함
            throw new IllegalStateException("Unauthenticated request: CurrentUser is missing.");
        }

        deactivateMeUseCase.deactivateMe(currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
