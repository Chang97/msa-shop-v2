package com.msashop.user.query.application.mapper;

import com.msashop.user.query.application.port.in.model.UserResult;
import com.msashop.user.query.application.port.out.model.UserWithRoleRow;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class UserQueryMapper {
    private UserQueryMapper() {}

    public static UserResult toResult(@NonNull List<UserWithRoleRow> rows) {
        // 1) user 기본 정보는 모든 row에서 동일하므로 첫 row를 기준으로 사용한다.
        UserWithRoleRow first = rows.get(0);
        // 2) roles 집계
        // - left join이므로 roleName이 null일 수 있다.
        // - 중복 제거를 위해 Set을 사용한다.
        // - 응답에서는 List로 내리므로 마지막에 List로 변환한다.
        Set<String> roleSet = new LinkedHashSet<>();
        for (UserWithRoleRow r : rows) {
            String roleName = r.getRoleName();
            if (roleName != null && !roleName.isBlank()) {
                roleSet.add(roleName.trim());
            }
        }
        List<String> roles = new ArrayList<>(roleSet);

        return new UserResult(
                first.getUserId(),
                first.getEmail(),
                first.getLoginId(),
                first.getUserName(),
                first.getEmpNo(),
                first.getPstnName(),
                first.getTel(),
                Boolean.TRUE.equals(first.getUseYn()),
                roles,
                first.getCreatedAt(),
                first.getCreatedBy(),
                first.getUpdatedAt(),
                first.getUpdatedBy()
        );

    }
}
