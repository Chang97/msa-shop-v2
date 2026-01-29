package com.msashop.user.application.service;

import com.msashop.user.application.port.in.ProvisionUserProfileUseCase;
import com.msashop.user.application.port.in.model.ProvisionUserProfileCommand;
import com.msashop.user.application.port.out.CreateUserProfilePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * auth-service에서 userId를 받아 user-service 프로필 row 생성.
 */
@Service
@RequiredArgsConstructor
public class ProvisionUserProfileService implements ProvisionUserProfileUseCase {

    private final CreateUserProfilePort createUserProfilePort;

    @Override
    @Transactional
    public void provision(ProvisionUserProfileCommand command) {
        // 이미 존재하면 멱등 처리(중복 호출/재시도까지 고려)
        createUserProfilePort.createIfAbsent(command);
    }
}

