package com.msashop.auth.application.port.out;

import com.msashop.auth.application.port.out.model.UserPofile;

/**
 * user-service에 프로필 생성 요청을 보내는 포트.
 */
public interface UserProfileProvisionPort {
    void provisionProfile(UserPofile profile);
}

