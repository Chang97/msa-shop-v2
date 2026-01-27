package com.msashop.user.query.adapter.in.web;

import com.msashop.common.web.exception.UnauthorizedException;
import com.msashop.common.web.exception.UserErrorCode;
import com.msashop.user.query.adapter.in.web.dto.UserMeResponse;
import com.msashop.user.query.adapter.in.web.mapper.UserWebQueryMapper;
import com.msashop.user.query.application.service.GetMeService;
import com.msashop.user.common.security.CurrentUser;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserQueryController {

    private final GetMeService getMeService;

    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> me(@AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null || currentUser.userId() == null) {
            // 공통 예외 처리 레이어에서 401로 매핑하는 방식 권장
            throw new UnauthorizedException(UserErrorCode.USER_CURRENT_MISSING);
        }

        return ResponseEntity.ok(UserWebQueryMapper.toResponse(getMeService.getMe(currentUser.userId())));
    }
}