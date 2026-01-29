package com.msashop.user.adapter.out.persistence.adapter;

import com.msashop.user.adapter.out.persistence.mapper.UserEntityMapper;
import com.msashop.user.adapter.out.persistence.repo.UserCommandJpaRepository;
import com.msashop.user.application.port.out.LoadUserPort;
import com.msashop.user.application.port.out.SaveUserPort;
import com.msashop.user.domain.model.User;
import com.msashop.user.adapter.out.persistence.entity.UserJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Command-side persistence adapter.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class UserCommandPersistenceAdapter implements LoadUserPort, SaveUserPort {

    private final UserCommandJpaRepository userCommandJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long userId) {
        return userCommandJpaRepository.findById(userId)
                .map(UserEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByAuthUserId(Long authUserId) {
        return userCommandJpaRepository.findByAuthUserId(authUserId)
                .map(UserEntityMapper::toDomain);
    }

    @Override
    public void save(User user) {
        UserJpaEntity entity = userCommandJpaRepository.findById(user.getUserId()).orElseThrow();
        UserEntityMapper.applyTo(user, entity);
        userCommandJpaRepository.save(entity);
    }

    @Override
    public void deactivate(User user) {
        // 1) 기존 엔티티 조회
        UserJpaEntity entity = userCommandJpaRepository.findById(user.getUserId())
                .orElseThrow(); // application에서 존재를 보장했으니 여기선 단순 처리

        entity.deactivate(); // 엔티티에 도메인 동작을 위임(또는 setUseYn(false))
    }

}
