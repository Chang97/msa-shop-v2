package com.msashop.user.application.service;

import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.common.web.exception.NotFoundException;
import com.msashop.user.application.port.in.DeactivateMeUseCase;
import com.msashop.user.application.port.out.DisableAuthUserPort;
import com.msashop.user.application.port.out.LoadUserPort;
import com.msashop.user.application.port.out.SaveUserPort;
import com.msashop.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DeactivateMe 유스케이스 구현체.
 */
@Service
@RequiredArgsConstructor
public class DeactivateMeService implements DeactivateMeUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final DisableAuthUserPort disableAuthUserPort;

    /**
     * 내 계정 비활성화는 CUD이므로 트랜잭션이 필요하다.
     */
    @Override
    @Transactional
    public void deactivateMe(Long userId) {
        User user = loadUserPort.findByAuthUserId(userId)
                .orElseThrow(() -> new NotFoundException(
                        CommonErrorCode.COMMON_NOT_FOUND,
                        "User not found. userId=" + userId
                ));

        // 도메인 상태 변경을 영속화
        saveUserPort.deactivate(user);

        // auth_db: auth_user_crdential enabled update
        disableAuthUserPort.disableAuthUser(user.getAuthUserId());

    }
}
