package com.msashop.user.query.adapter.out.persistence.adapter;

import com.msashop.user.query.adapter.out.persistence.repo.UserQueryJpaRepository;
import com.msashop.user.query.application.port.out.LoadUserWithRolesPort;
import com.msashop.user.query.application.port.out.model.UserWithRoleRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
@Component
@RequiredArgsConstructor
public class UserQueryPersistenceAdapter implements LoadUserWithRolesPort {
    private final UserQueryJpaRepository userQueryJpaRepository;

    @Override
    public List<UserWithRoleRow> findMeWithRoles(Long userId) {
        return userQueryJpaRepository.findUserWithRolesRows(userId);
    }
}
