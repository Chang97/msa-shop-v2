package com.msashop.auth.adapter.out.persistence.adapter;

import com.msashop.auth.adapter.out.persistence.entity.AuthUserCredentialJpaEntity;
import com.msashop.auth.adapter.out.persistence.entity.RoleJpaEntity;
import com.msashop.auth.adapter.out.persistence.entity.UserRoleMapJpaEntity;
import com.msashop.auth.adapter.out.persistence.mapper.AuthUserCredentialMapper;
import com.msashop.auth.adapter.out.persistence.repo.AuthUserCredentialJpaRepository;
import com.msashop.auth.adapter.out.persistence.repo.RoleJpaRepository;
import com.msashop.auth.adapter.out.persistence.repo.UserRoleMapJpaRepository;
import com.msashop.auth.application.port.out.CredentialPort;
import com.msashop.auth.application.port.out.LoadUserPort;
import com.msashop.auth.application.port.out.UserRolePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional(readOnly = true)
@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements LoadUserPort, CredentialPort, UserRolePort {
    private final AuthUserCredentialJpaRepository credentialRepo;
    private final UserRoleMapJpaRepository userRoleMapRepo;
    private final RoleJpaRepository roleJpaRepository;

    @Override
    public Optional<AuthUserRecord> findByLoginId(String loginId) {
        return credentialRepo.findByLoginId(loginId)
                .map(e -> AuthUserCredentialMapper.toAuthRecord(
                        e,
                        userRoleMapRepo.findRoleNamesByUserId(e.getUserId())
                ));
    }

    @Override
    public boolean existsByEmail(String email) {
        return credentialRepo.existsByEmail(email);
    }

    @Override
    public boolean existsByLoginId(String loginId) {
        return credentialRepo.existsByLoginId(loginId);
    }

    @Override
    @Transactional
    public Long saveCredential(String email, String loginId, String passwordHash) {
        AuthUserCredentialJpaEntity saved = credentialRepo.save(
                AuthUserCredentialJpaEntity.builder()
                        .email(email)
                        .loginId(loginId)
                        .passwordHash(passwordHash)
                        .build()
        );
        return saved.getUserId();
    }

    @Override
    @Transactional
    public void disable(Long authUserId) {
        AuthUserCredentialJpaEntity entity = credentialRepo.findById(authUserId)
                .orElseThrow();
        entity.disable();
    }

    @Override
    @Transactional
    public void assignRole(Long userId, String roleName) {
        RoleJpaEntity role = roleJpaRepository.findByRoleName(roleName)
                .orElseGet(() -> roleJpaRepository.save(
                        RoleJpaEntity.builder()
                                .roleName(roleName)
                                .useYn(true)
                                .build()
                ));

        if (!userRoleMapRepo.existsByUserIdAndRoleId(userId, role.getRoleId())) {
            userRoleMapRepo.save(
                    UserRoleMapJpaEntity.builder()
                            .userId(userId)
                            .roleId(role.getRoleId())
                            .build()
            );
        }
    }
}
