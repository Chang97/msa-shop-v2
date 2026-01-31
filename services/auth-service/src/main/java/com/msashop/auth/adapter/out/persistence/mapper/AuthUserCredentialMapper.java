package com.msashop.auth.adapter.out.persistence.mapper;

import com.msashop.auth.adapter.out.persistence.entity.AuthUserCredentialJpaEntity;
import com.msashop.auth.application.port.out.LoadUserPort.AuthUserRecord;

import java.util.List;

public final class AuthUserCredentialMapper {
    private AuthUserCredentialMapper() {}

    public static AuthUserRecord toAuthRecord(AuthUserCredentialJpaEntity e, List<String> roles) {
        return new AuthUserRecord(
                e.getUserId(),
                e.getEmail(),
                e.getLoginId(),
                e.getPasswordHash(),
                e.getEnabled(),
                roles
        );
    }
}

