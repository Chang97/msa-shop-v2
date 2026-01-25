package com.msashop.auth.command.adapter.out.persistence.adapter;

import com.msashop.auth.command.adapter.out.persistence.entity.UserEntity;
import com.msashop.auth.command.adapter.out.persistence.mapper.UserEntityMapper;
import com.msashop.auth.command.adapter.out.persistence.repo.UserJpaRepository;
import com.msashop.auth.command.application.port.out.LoadUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements LoadUserPort {
    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<AuthUserRecord> findByLoginId(String loginId) {
        return userJpaRepository.findByLoginId(loginId)
                .map(UserEntityMapper::toAuthRecord);
    }
}
