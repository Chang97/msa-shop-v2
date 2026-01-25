package com.msashop.auth.command.application.port.in.model;

public record LoginCommand (
        String loginId,
        String password
) {}
