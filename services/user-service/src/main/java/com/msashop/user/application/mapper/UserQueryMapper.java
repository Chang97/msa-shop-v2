package com.msashop.user.application.mapper;

import com.msashop.user.application.port.in.model.UserResult;
import com.msashop.user.application.port.out.model.UserRow;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;

public final class UserQueryMapper {
    private UserQueryMapper() {}

    public static UserResult toResult(@NonNull UserRow row) {
        return new UserResult(
                row.getUserId(),
                row.getAuthUserId(),
                row.getUserName(),
                row.getEmpNo(),
                row.getPstnName(),
                row.getTel(),
                Boolean.TRUE.equals(row.getUseYn()),
                row.getCreatedAt(),
                row.getCreatedBy(),
                row.getUpdatedAt(),
                row.getUpdatedBy()
        );

    }
}

