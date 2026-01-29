package com.msashop.user.application.port.in.model;

public record ProvisionUserProfileCommand(
        Long authUserId,
        String userName,
        String empNo,
        String pstnName,
        String tel
) {}

