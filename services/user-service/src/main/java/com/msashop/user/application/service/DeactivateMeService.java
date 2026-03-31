package com.msashop.user.application.service;

import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.user.application.event.UserSagaEventFactory;
import com.msashop.user.application.port.in.DeactivateMeUseCase;
import com.msashop.user.application.port.out.LoadUserPort;
import com.msashop.user.application.port.out.OutboxEventPort;
import com.msashop.user.application.port.out.SaveUserPort;
import com.msashop.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeactivateMeService implements DeactivateMeUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final OutboxEventPort outboxEventPort;
    private final UserSagaEventFactory eventFactory;

    @Override
    @Transactional
    public void deactivateMe(Long userId) {
        User user = loadUserPort.findByAuthUserId(userId)
                .orElseThrow(() -> new BusinessException(
                        CommonErrorCode.COMMON_NOT_FOUND,
                        "사용자를 찾을 수 없습니다. userId=" + userId
                ));

        if (!user.isUseYn()) {
            return;
        }

        saveUserPort.deactivate(user);
        outboxEventPort.append(eventFactory.userDeactivated(user.getAuthUserId(), user.getUserId()));
    }
}
