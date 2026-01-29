package com.msashop.user.application.service;

import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.common.web.exception.ConflictException;
import com.msashop.common.web.exception.NotFoundException;
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
                .orElseThrow(() -> new NotFoundException(
                        CommonErrorCode.COMMON_NOT_FOUND, "User not found. userId = " + userId
                ));

        // 이미 비활성화 계정은 수정 불가(정책)
        if (!user.isUseYn()) {
            throw new ConflictException(
                    CommonErrorCode.COMMON_CONFLICT,
                    "Deactivated user cannot be updated. userId=" + userId
            );
        }

        user.updateProfile(command.userName(), command.empNo(), command.psntName(), command.tel());

        saveUserPort.save(user);
    }
}
