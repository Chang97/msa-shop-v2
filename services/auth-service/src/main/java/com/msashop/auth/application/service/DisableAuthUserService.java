package com.msashop.auth.application.service;

import com.msashop.auth.application.port.in.DisableAuthUserUseCase;
import com.msashop.auth.application.port.out.CredentialPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DisableAuthUserService implements DisableAuthUserUseCase {
    private final CredentialPort credentialPort;

    @Override
    public void disableAuthUser(Long authUserId) {
        credentialPort.disable(authUserId);
    }
}
