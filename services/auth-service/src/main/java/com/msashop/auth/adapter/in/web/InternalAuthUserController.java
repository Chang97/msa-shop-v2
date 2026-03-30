package com.msashop.auth.adapter.in.web;

import com.msashop.auth.application.port.in.DisableAuthUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/auth/users")
@RequiredArgsConstructor
public class InternalAuthUserController {

    private final DisableAuthUserUseCase disableAuthUserUseCase;

    @PatchMapping("/{authUserId}/disable")
    public ResponseEntity<Void> deactivateMe(@PathVariable Long authUserId) {
        disableAuthUserUseCase.disableAuthUser(authUserId);
        return ResponseEntity.noContent().build();
    }
}
