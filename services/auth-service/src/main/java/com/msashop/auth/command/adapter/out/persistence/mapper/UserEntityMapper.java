package com.msashop.auth.command.adapter.out.persistence.mapper;

import com.msashop.auth.command.adapter.out.persistence.entity.UserEntity;
import com.msashop.auth.command.application.port.out.LoadUserPort.AuthUserRecord;

public final class UserEntityMapper {
    private UserEntityMapper() {}

    public static AuthUserRecord toAuthRecord(UserEntity e) {
        return new AuthUserRecord(
                e.getUserId(),
                e.getEmail(),
                e.getLoginId(),
                e.getUserPassword(),
                e.getUseYn()
        );
    }
}
