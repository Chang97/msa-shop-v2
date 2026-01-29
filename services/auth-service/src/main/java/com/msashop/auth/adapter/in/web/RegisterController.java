package com.msashop.auth.adapter.in.web;


import com.msashop.auth.adapter.in.web.dto.RegisterRequest;
import com.msashop.auth.adapter.in.web.dto.RegisterResponse;
import com.msashop.auth.adapter.in.web.mapper.AuthWebMapper;
import com.msashop.auth.application.port.in.RegisterUseCase;
import com.msashop.auth.application.port.in.model.RegisterCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RegisterController {
    private final RegisterUseCase registerUseCase;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        Long userId = registerUseCase.register(AuthWebMapper.toRegisterCommand(request));

        return ResponseEntity.ok(new RegisterResponse(userId, Instant.now()));
    }
}
