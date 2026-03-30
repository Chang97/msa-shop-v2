package com.msashop.user.application.service;

import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.user.application.port.in.UpdateMeUseCase;
import com.msashop.user.application.port.in.model.UpdateMeCommand;
import com.msashop.user.application.port.out.LoadUserPort;
import com.msashop.user.application.port.out.SaveUserPort;
import com.msashop.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내 정보 수정 유스케이스 구현체.
 */
@Service
@RequiredArgsConstructor
public class UpdateMeService implements UpdateMeUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;

    @Override
    @Transactional
    public void updateMe(Long userId, UpdateMeCommand command) {
        User user = loadUserPort.findByAuthUserId(userId)
                .orElseThrow(() -> new BusinessException(
                        CommonErrorCode.COMMON_NOT_FOUND, "사용자를 찾을 수 없습니다. userId = " + userId
                ));

        // 이미 비활성화 계정은 수정 불가(정책)
        if (!user.isUseYn()) {
            throw new BusinessException(
                    CommonErrorCode.COMMON_CONFLICT,
                    "비활성화된 사용자는 수정할 수 없습니다. userId=" + userId
            );
        }

        user.updateProfile(command.userName(), command.empNo(), command.psntName(), command.tel());

        saveUserPort.save(user);
    }
}
