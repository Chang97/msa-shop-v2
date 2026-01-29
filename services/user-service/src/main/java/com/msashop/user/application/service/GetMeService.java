package com.msashop.user.application.service;

import com.msashop.common.web.exception.NotFoundException;
import com.msashop.common.web.exception.UserErrorCode;
import com.msashop.user.application.mapper.UserQueryMapper;
import com.msashop.user.application.port.in.GetMeUseCase;
import com.msashop.user.application.port.in.model.UserResult;
import com.msashop.user.application.port.out.LoadUserWithRolesPort;
import com.msashop.user.application.port.out.model.UserRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Query 유스케이스 서비스.
 *
 * 역할:
 * - Repo에서 조인 Row List를 받는다.
 * - Row List를 집계해 UserResult 1건으로 만든다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetMeService implements GetMeUseCase {

    private final LoadUserWithRolesPort loadUserWithRolesPort;

    /**
     * 내 정보 조회 (/me).
     */
    @Override
    public UserResult getMe(Long userId) {
        UserRow row = loadUserWithRolesPort.findByAuthUserId(userId)
                .orElseThrow(() -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));

        return UserQueryMapper.toResult(row);
    }
}
