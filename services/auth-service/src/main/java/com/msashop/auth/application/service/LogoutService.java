package com.msashop.auth.application.service;

import com.msashop.auth.application.port.in.LogoutUseCase;
import com.msashop.auth.application.port.in.model.LogoutCommand;
import com.msashop.auth.application.port.out.RefreshTokenPort;
import com.msashop.auth.application.service.token.RefreshTokenParser;
import com.msashop.auth.application.service.token.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {
    private final RefreshTokenPort refreshTokenPort;
    private final RefreshTokenParser refreshTokenParser;
    private final TokenHasher tokenHasher;

    @Override
    @Transactional
    public void logout(LogoutCommand command) {
        Instant now = Instant.now();

        String raw = command.refreshToken();
        if (raw == null || raw.isBlank()) {
            return;
        }

        final String validRaw;
        try {
            validRaw = refreshTokenParser.validate(raw);
        } catch (RuntimeException e) {
            return;
        }

        String tokenHash = tokenHasher.sha256Hex(validRaw);

        var storedOpt = refreshTokenPort.findActiveByTokenHash(tokenHash, now);
        if (storedOpt.isEmpty()) {
            return;
        }

        refreshTokenPort.revoke(tokenHash);
    }
}

