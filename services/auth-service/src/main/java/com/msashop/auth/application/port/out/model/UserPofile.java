package com.msashop.auth.application.port.out.model;

public record UserPofile(
        Long authUserId,
        String userName,
        String empNo,
        String pstnName,
        String tel
) {
}
