package com.msashop.auth.command.application.service;

import com.msashop.auth.command.application.port.in.RefreshUseCase;
import com.msashop.auth.command.application.port.in.model.RefreshCommand;
import com.msashop.auth.command.application.port.in.model.RefreshResult;
import com.msashop.auth.command.application.port.out.RefreshTokenPort;
import com.msashop.auth.command.application.service.token.RefreshTokenGenerator;
import com.msashop.auth.command.application.service.token.RefreshTokenParser;
import com.msashop.auth.command.application.service.token.TokenHasher;
import com.msashop.auth.command.application.service.token.TokenIssuer;
import com.msashop.auth.config.auth.RefreshTokenProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefreshService implements RefreshUseCase {
    private final RefreshTokenPort refreshTokenPort;

    private final RefreshTokenParser refreshTokenParser;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final TokenHasher tokenHasher;
    private final RefreshTokenProperties refreshTokenProperties;

    private final TokenIssuer tokenIssuer;

    @Override
    @Transactional
    public RefreshResult refresh(RefreshCommand command) {
        Instant now = Instant.now();

        // 1) tokenId 추출 (raw = tokenId.secret)
        String raw = command.refreshToken();
        String tokenId = refreshTokenParser.extractTokenId(raw);

        // 2) raw 해시 계산 (DB에는 hash만 저장되어 있으므로 비교 필요)
        String rawHash = tokenHasher.sha256Hex(raw);

        // 3) DB에서 active refresh 조회 (revoked=false AND expires_at > now)
        var storedActive = refreshTokenPort.findActiveByTokenId(tokenId, now);
        if (storedActive.isEmpty()) {
            // tokenId 자체가 존재하는지 확인 (재사용/만료 구분)
            boolean exists = refreshTokenPort.findByTokenId(tokenId).isPresent();
            if (exists) {
                // 이미 revoke 되었는데 다시 들어옴 -> replay 가능성
                throw new IllegalArgumentException("Refresh token already used (replay suspected)");
            }
            throw new IllegalArgumentException("Invalid refresh token");
        }
        var stored = storedActive.get();

        // 4) hash 비교로 위변조/오입력 방지
        if (!rawHash.equals(stored.tokenHash())) {
            // 1차: 단순 에러. (2차: 재사용/탈취 탐지 후 전체 세션 폐기 같은 대응 가능)
            throw new IllegalArgumentException("Invalid refresh token");
        }

        // 5) rotate: 기존 refresh revoke 처리
        var generatedNew = refreshTokenGenerator.generate();
        String newRaw = generatedNew.rawToken();
        String newHash = tokenHasher.sha256Hex(newRaw);

        Instant newExpiresAt = now.plusSeconds(refreshTokenProperties.ttlSeconds());

        // last_used_at 업데이트
        refreshTokenPort.markUsed(stored.tokenId(), now);

        refreshTokenPort.revoke(
                stored.tokenId(),
                now,
                stored.userId(),        // 1차: 본인 revoke로 처리(감사 컬럼은 AuditorAware가 채움)
                generatedNew.tokenId()  // replaced_by_token_id
        );

        // 6) 신규 refresh 저장
        refreshTokenPort.save(new RefreshTokenPort.NewRefreshToken(
                generatedNew.tokenId(),
                newHash,
                stored.userId(),
                newExpiresAt
        ));


        // 8) access 재발급
        String newAccess = tokenIssuer.issueAccessToken(stored.userId(), List.of("ROLE_USER"));

        return new RefreshResult(newAccess, newRaw);
    }
}
