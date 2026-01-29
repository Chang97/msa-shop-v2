package com.msashop.auth.application.port.in.model;

public record LoginCommand (
        String loginId,
        String password
) {}

