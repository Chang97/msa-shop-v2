package com.msashop.auth.command.application.service;

import com.msashop.auth.command.application.port.in.LogoutUseCase;
import com.msashop.auth.command.application.port.in.model.LogoutCommand;
import com.msashop.auth.command.application.port.out.RefreshTokenPort;
import com.msashop.auth.command.application.service.token.RefreshTokenParser;
import com.msashop.auth.command.application.service.token.TokenHasher;
import com.msashop.auth.common.exception.ErrorCode;
import com.msashop.auth.common.exception.UnauthorizedException;
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
            return; // 할 게 없으면 그냥 종료
        }

        final String tokenId;
        try {
            tokenId = refreshTokenParser.extractTokenId(raw);
        } catch (RuntimeException e) {
            return; // 포맷이 이상해도 "정리" 목적이므로 조용히 종료(필요하면 warn 로그만)
        }

        String rawHash = tokenHasher.sha256Hex(raw);

        var storedOpt = refreshTokenPort.findActiveByTokenId(tokenId, now);
        if (storedOpt.isEmpty()) {
            return;
        }

        var stored = storedOpt.get();
        // 해시 불일치(위변조/오입력)도 조용히 종료
        // (보안 이벤트로 보고 싶으면 warn 로그 남기고 return)
        if (!rawHash.equals(stored.tokenHash())) {
            return;
        }

        // revoke는 idempotent하게 구현되어 있어야 함(이미 revoked여도 문제없게)
        refreshTokenPort.revoke(tokenId, now, null);
    }
}
