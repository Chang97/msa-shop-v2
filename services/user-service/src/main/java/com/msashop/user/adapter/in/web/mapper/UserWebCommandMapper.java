package com.msashop.user.adapter.in.web.mapper;

import com.msashop.user.adapter.in.web.dto.UserMeResponse;
import com.msashop.user.adapter.in.web.dto.UserMeUpdateRequest;
import com.msashop.user.application.port.in.model.UpdateMeCommand;
import org.springframework.stereotype.Component;

/**
 * Query 결과(UserResult) -> Web 응답(UserMeResponse) 변환.
 */
@Component
public final class UserWebCommandMapper {
    private UserWebCommandMapper() {}

    public static UpdateMeCommand toUpdateMeCommand(UserMeUpdateRequest r) {
        return new UpdateMeCommand(
                r.userName(),
                r.empNo(),
                r.pstnName(),
                r.tel()
        );
    }
}
