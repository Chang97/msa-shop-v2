package com.msashop.user.query.application.service;

import com.msashop.common.web.exception.NotFoundException;
import com.msashop.common.web.exception.UserErrorCode;
import com.msashop.user.query.adapter.out.persistence.repo.UserQueryJpaRepository;
import com.msashop.user.query.application.mapper.UserQueryMapper;
import com.msashop.user.query.application.port.in.GetMeUseCase;
import com.msashop.user.query.application.port.in.model.UserResult;
import com.msashop.user.query.application.port.out.LoadUserWithRolesPort;
import com.msashop.user.query.application.port.out.model.UserWithRoleRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Query 유스케이스 서비스.
 *
 * 역할:
 * - Repo에서 조인 Row List를 받는다.
 * - Row List를 집계해 "UserResult 1건 + roles List"로 만든다.
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
        List<UserWithRoleRow> rows =
                loadUserWithRolesPort.findMeWithRoles(userId);

        // 1) user가 없으면 rows 자체가 비어있다.
        if (rows.isEmpty()) {
            throw new NotFoundException(UserErrorCode.USER_NOT_FOUND);
        }

        return UserQueryMapper.toResult(rows);
    }
}