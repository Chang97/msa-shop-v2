package com.msashop.auth.command.application.service;

import com.msashop.auth.command.application.port.in.LogoutUseCase;
import com.msashop.auth.command.application.port.in.model.LogoutCommand;
import com.msashop.auth.command.application.port.out.RefreshTokenPort;
import com.msashop.auth.command.application.service.token.RefreshTokenParser;
import com.msashop.auth.command.application.service.token.TokenHasher;
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
        String tokenId = refreshTokenParser.extractTokenId(raw);
        String rawHash = tokenHasher.sha256Hex(raw);

        var stored = refreshTokenPort.findActiveByTokenId(tokenId, now)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (!rawHash.equals(stored.tokenHash())) throw new IllegalArgumentException("Invalid refresh token");

        // 1차: tokenId만으로 revoke
        // - 엄밀하게는 rawHash까지 비교하고 revoke하는게 더 안전
        refreshTokenPort.revoke(tokenId, now, 0L, null);
    }
}
