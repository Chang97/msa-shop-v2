package com.msashop.user.adapter.out.persistence.adapter;

import com.msashop.user.adapter.out.persistence.repo.UserQueryJpaRepository;
import com.msashop.user.application.port.out.LoadUserPort;
import com.msashop.user.application.port.out.LoadUserWithRolesPort;
import com.msashop.user.application.port.out.model.UserRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional(readOnly = true)
@Component
@RequiredArgsConstructor
public class UserQueryPersistenceAdapter implements LoadUserWithRolesPort {
    private final UserQueryJpaRepository userQueryJpaRepository;

    @Override
    public Optional<UserRow> findByAuthUserId(Long userId) {
        return userQueryJpaRepository.findByAuthUserId(userId);
    }
}
