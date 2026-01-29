package com.msashop.user.adapter.out.persistence.adapter;

import com.msashop.user.adapter.out.persistence.entity.UserJpaEntity;
import com.msashop.user.adapter.out.persistence.repo.UserCommandJpaRepository;
import com.msashop.user.application.port.in.model.ProvisionUserProfileCommand;
import com.msashop.user.application.port.out.CreateUserProfilePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserProfileCreatePersistenceAdapter implements CreateUserProfilePort {

    private final UserCommandJpaRepository userCommandJpaRepository;

    @Override
    public void createIfAbsent(ProvisionUserProfileCommand command) {
        if (userCommandJpaRepository.existsByAuthUserId(command.authUserId())) {
            return; // 멱등
        }

        UserJpaEntity entity = UserJpaEntity.builder()
                .authUserId(command.authUserId())
                .userName(command.userName())
                .empNo(command.empNo())
                .pstnName(command.pstnName())
                .tel(command.tel())
                .useYn(true)
                .build();

        userCommandJpaRepository.save(entity);
    }
}
