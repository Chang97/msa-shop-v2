package com.msashop.user.application.port.out;

import com.msashop.user.application.port.in.model.ProvisionUserProfileCommand;

public interface CreateUserProfilePort {
    void createIfAbsent(ProvisionUserProfileCommand command);
}
