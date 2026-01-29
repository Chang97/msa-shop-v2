package com.msashop.auth.application.service;

import com.msashop.auth.application.port.in.RegisterUseCase;
import com.msashop.auth.application.port.in.model.RegisterCommand;
import com.msashop.auth.application.port.out.CredentialPort;
import com.msashop.auth.application.port.out.UserProfileProvisionPort;
import com.msashop.auth.application.port.out.UserRolePort;
import com.msashop.auth.application.port.out.model.UserPofile;
import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.common.web.exception.ConflictException;
import com.msashop.common.web.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 유스케이스 구현.
 */
@Service
@RequiredArgsConstructor
public class RegisterService implements RegisterUseCase {

    private final CredentialPort credentialPort;
    private final UserRolePort userRolePort;
    private final UserProfileProvisionPort userProfileProvisionPort;

    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Long register(RegisterCommand command) {
        // 1) 입력 기본 검증(도메인/서비스 규칙)
        if (command.email() == null || command.loginId() == null || command.rawPassword() == null) {
            throw new ValidationException(CommonErrorCode.COMMON_VALIDATION, "Missing required fields.");
        }

        // 2) 중복 체크
        if (credentialPort.existsByEmail(command.email())) {
            throw new ConflictException(CommonErrorCode.COMMON_CONFLICT, "Email already exists.");
        }
        if (credentialPort.existsByLoginId(command.loginId())) {
            throw new ConflictException(CommonErrorCode.COMMON_CONFLICT, "LoginId already exists.");
        }

        // 3) 비밀번호 해시
        String hash = passwordEncoder.encode(command.rawPassword());

        // 4) credential 저장(authUserId 생성)
        Long userId = credentialPort.saveCredential(command.email(), command.loginId(), hash);

        // 5) 기본 역할 부여
        userRolePort.assignRole(userId, "ROLE_USER");

        // 6) user-service에 프로필 row 생성(동기 호출)
        // - TODO: 실패 시 롤백할지/보상 트랜잭션으로 갈지 정책 필요
        // - MVP에서는 실패하면 예외로 전체 실패 처리하는 게 단순
        userProfileProvisionPort.provisionProfile(new UserPofile(
                userId,
                command.userName(),
                command.empNo(),
                command.pstnName(),
                command.tel()
        ));

        return userId;
    }

}
