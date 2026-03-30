package com.msashop.auth.application.service;

import com.msashop.auth.adapter.in.web.dto.AuthMeResponse;
import com.msashop.auth.application.port.in.GetAuthMeUseCase;
import com.msashop.auth.application.port.out.LoadUserPort;
import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetAuthMeService implements GetAuthMeUseCase {

    private final LoadUserPort loadUserPort;

    @Override
    @Transactional(readOnly = true)
    public AuthMeResponse getMe(Long authUserId) {
        var user = loadUserPort.findByUserId(authUserId)
                .orElseThrow(() -> new BusinessException(
                        CommonErrorCode.COMMON_NOT_FOUND,
                        "인증 사용자 정보를 찾을 수 없습니다. authUserId=" + authUserId
                ));

        return new AuthMeResponse(
                user.userId(),
                user.email(),
                user.loginId(),
                Boolean.TRUE.equals(user.enabled()),
                user.roles() == null ? java.util.List.of() : user.roles()
        );
    }
}
