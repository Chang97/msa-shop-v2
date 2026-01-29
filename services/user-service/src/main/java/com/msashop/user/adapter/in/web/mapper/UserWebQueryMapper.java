package com.msashop.user.adapter.in.web.mapper;

import com.msashop.user.adapter.in.web.dto.UserMeResponse;
import com.msashop.user.application.port.in.model.UserResult;
import org.springframework.stereotype.Component;

/**
 * Query 결과(UserResult) -> Web 응답(UserMeResponse) 변환.
 */
@Component
public final class UserWebQueryMapper {
    private UserWebQueryMapper() {}

    public static UserMeResponse toResponse(UserResult r) {
        return new UserMeResponse(
                r.userId(),
                r.authUserId(),
                r.userName(),
                r.empNo(),
                r.pstnName(),
                r.tel(),
                r.useYn(),
                r.createdAt(),
                r.createdBy(),
                r.updatedAt(),
                r.updatedBy()
        );
    }
}

