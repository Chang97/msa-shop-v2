package com.msashop.user.adapter.in.web;

import com.msashop.user.adapter.in.web.dto.ProvisionUserProfileRequest;
import com.msashop.user.application.port.in.ProvisionUserProfileUseCase;
import com.msashop.user.application.port.in.model.ProvisionUserProfileCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 내부용 엔드포인트.
 * - gateway 라우팅에서 제외하거나, 내부 네트워크에서만 접근되도록 제한 필요.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/users")
public class InternalUserProvisionController {

    private final ProvisionUserProfileUseCase useCase;

    @PostMapping
    public ResponseEntity<Void> provision(@Valid @RequestBody ProvisionUserProfileRequest request) {
        useCase.provision(new ProvisionUserProfileCommand(
                request.authUserId(),
                request.userName(),
                request.empNo(),
                request.pstnName(),
                request.tel()
        ));
        return ResponseEntity.noContent().build();
    }
}
