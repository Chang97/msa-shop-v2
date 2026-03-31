package com.msashop.user.adapter.in.web;

import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.UserErrorCode;
import com.msashop.user.adapter.in.web.dto.UserMeUpdateRequest;
import com.msashop.user.adapter.in.web.mapper.UserWebCommandMapper;
import com.msashop.user.application.port.in.DeactivateMeUseCase;
import com.msashop.user.application.port.in.UpdateMeUseCase;
import com.msashop.user.application.port.in.model.UpdateMeCommand;
import com.msashop.user.common.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final UpdateMeUseCase updateMeUseCase;

    /**
     * 내 계정 비활성화.
     * - gateway에서 내려준 X-User-Id 기반으로 CurrentUser가 세팅되어야 한다.
     */
    @PatchMapping("/me/deactivate")
    public ResponseEntity<Void> deactivateMe(@AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null || currentUser.userId() == null) {
            throw new BusinessException(UserErrorCode.USER_CURRENT_MISSING);
        }

        deactivateMeUseCase.deactivateMe(currentUser.userId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me")
    public ResponseEntity<Void> updateMe(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody UserMeUpdateRequest request
            ) {
        if (currentUser == null || currentUser.userId() == null) {
            throw new BusinessException(UserErrorCode.USER_CURRENT_MISSING);
        }

        updateMeUseCase.updateMe(
                currentUser.userId(),
                UserWebCommandMapper.toUpdateMeCommand(request)
        );

        return ResponseEntity.noContent().build();
    }
}
