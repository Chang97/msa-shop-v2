package com.msashop.user.command.adapter.out.persistence.adapter;

import com.msashop.user.command.adapter.out.persistence.repo.UserCommandJpaRepository;
import com.msashop.user.command.application.port.out.LoadUserPort;
import com.msashop.user.command.application.port.out.SaveUserPort;
import com.msashop.user.command.domain.model.User;
import com.msashop.user.entity.UserJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Command-side persistence adapter.
 */
@Component
@RequiredArgsConstructor
public class UserCommandPersistenceAdapter implements LoadUserPort, SaveUserPort {

    private final UserCommandJpaRepository userCommandJpaRepository;

    @Override
    public Optional<User> findById(Long userId) {
        return userCommandJpaRepository.findById(userId)
                .map(this::toDomain);
    }

    @Override
    public void save(User user) {
        // 기존 엔티티를 다시 조회해서 상태만 갱신하는 방식(가장 단순)
        UserJpaEntity entity = userCommandJpaRepository.findById(user.userId())
                .orElseThrow(); // application에서 존재를 보장했으니 여기선 단순 처리

        entity.deactivate(); // 엔티티에 도메인 동작을 위임(또는 setUseYn(false))
        userCommandJpaRepository.save(entity);
    }

    private User toDomain(UserJpaEntity e) {
        return new User(
                e.getUserId(),
                Boolean.TRUE.equals(e.getUseYn()),
                e.getUpdatedAt(),
                e.getUpdatedBy()
        );
    }
}
