package com.msashop.user.adapter.out.persistence.adapter;

import com.msashop.user.adapter.out.persistence.entity.UserJpaEntity;
import com.msashop.user.adapter.out.persistence.repo.UserCommandJpaRepository;
import com.msashop.user.application.port.in.model.ProvisionUserProfileCommand;
import com.msashop.user.application.port.out.CreateUserProfilePort;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserProfileCreatePersistenceAdapter implements CreateUserProfilePort {

    private final UserCommandJpaRepository userCommandJpaRepository;

    @Override
    public Long createIfAbsent(ProvisionUserProfileCommand command) {
        // 1. 먼저 실제 row를 한 번만 조회한다.
        // 이미 존재하면 그 row의 userId를 그대로 반환하면 된다.
        return userCommandJpaRepository.findByAuthUserId(command.authUserId())
                .map(UserJpaEntity::getUserId)
                .orElseGet(() -> createNewUser(command));
    }

    /**
     * 존재하지 않을 때만 새 users row를 생성한다.
     * 동시 요청으로 unique constraint 충돌이 나면 이미 다른 트랜잭션이 만든 것으로 보고 재조회한다.
     */
    private Long createNewUser(ProvisionUserProfileCommand command) {
        try {
            UserJpaEntity entity = UserJpaEntity.builder()
                    .authUserId(command.authUserId())
                    .userName(command.userName())
                    .empNo(command.empNo())
                    .pstnName(command.pstnName())
                    .tel(command.tel())
                    .useYn(true)
                    .build();

            return userCommandJpaRepository.save(entity).getUserId();
        } catch (DataIntegrityViolationException e) {
            // auth_user_id unique 제약으로 충돌했다면
            // 같은 이벤트를 다른 재시도/스레드가 먼저 반영한 것으로 간주하고 기존 row를 다시 읽는다.
            return userCommandJpaRepository.findByAuthUserId(command.authUserId())
                    .map(UserJpaEntity::getUserId)
                    .orElseThrow(() -> new IllegalStateException(
                            "users row 저장 충돌 후 재조회에 실패했습니다. authUserId=" + command.authUserId(), e
                    ));
        }
    }
}
