package com.msashop.user.application.service;

import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.UserErrorCode;
import com.msashop.user.application.port.in.GetMeUseCase;
import com.msashop.user.application.port.in.model.UserResult;
import com.msashop.user.application.port.out.LoadUserPort;
import com.msashop.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the current user's profile and converts it to an application result model.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetMeService implements GetMeUseCase {

    private final LoadUserPort loadUserPort;

    /**
     * Loads the current user by auth user id and maps it to the response model.
     */
    @Override
    public UserResult getMe(Long userId) {
        User user = loadUserPort.findByAuthUserId(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        return new UserResult(
                user.getUserId(),
                user.getAuthUserId(),
                user.getUserName(),
                user.getEmpNo(),
                user.getPstnName(),
                user.getTel(),
                user.isUseYn(),
                user.getCreatedAt(),
                user.getCreatedBy(),
                user.getUpdatedAt(),
                user.getUpdatedBy()
        );
    }
}
