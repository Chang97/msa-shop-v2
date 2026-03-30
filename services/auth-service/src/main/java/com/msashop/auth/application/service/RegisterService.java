package com.msashop.auth.application.service;

import com.msashop.auth.application.event.AuthUserSagaEventFactory;
import com.msashop.auth.application.port.in.RegisterUseCase;
import com.msashop.auth.application.port.in.model.RegisterCommand;
import com.msashop.auth.application.port.out.CredentialPort;
import com.msashop.auth.application.port.out.OutboxEventPort;
import com.msashop.auth.application.port.out.UserRolePort;
import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 회원가입 유스케이스 구현.
 */
@Service
@RequiredArgsConstructor
public class RegisterService implements RegisterUseCase {

    private final CredentialPort credentialPort;
    private final UserRolePort userRolePort;
    private final OutboxEventPort outboxEventPort;

    private final PasswordEncoder passwordEncoder;
    private final AuthUserSagaEventFactory eventFactory;

    @Override
    @Transactional
    public Long register(RegisterCommand command) {
        // 1) 입력 기본 검증(도메인/서비스 규칙)
        if (command.email() == null || command.loginId() == null || command.rawPassword() == null) {
            throw new BusinessException(CommonErrorCode.COMMON_VALIDATION, "필수 입력값이 누락되었습니다.");
        }

        // 2) 중복 체크
        if (credentialPort.existsByEmail(command.email())) {
            throw new BusinessException(CommonErrorCode.COMMON_CONFLICT, "이미 사용 중인 이메일입니다.");
        }
        if (credentialPort.existsByLoginId(command.loginId())) {
            throw new BusinessException(CommonErrorCode.COMMON_CONFLICT, "이미 사용 중인 로그인 아이디입니다.");
        }

        // 3) 비밀번호 해시
        String hash = passwordEncoder.encode(command.rawPassword());

        // 4) credential 저장(authUserId 생성)
        Long authUserId = credentialPort.saveDisabledCredential(command.email(), command.loginId(), hash);

        // 5) 기본 역할 부여
        userRolePort.assignRole(authUserId, "ROLE_USER");

        // 6) 사용자 등록 이벤트
        // 회원가입 플로우 전체를 묶는 saga/correlation ID를 생성
        String sagaId = UUID.randomUUID().toString();

        // 같은 DB 트랜잭션 안에서 outbox에 적재해야 메시지 유실이 없음.
        outboxEventPort.append(eventFactory.authUserCreated(
                sagaId,
                authUserId,
                command
        ));


        return authUserId;
    }

}
