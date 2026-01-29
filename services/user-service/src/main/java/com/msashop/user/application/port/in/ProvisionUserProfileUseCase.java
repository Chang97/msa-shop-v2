package com.msashop.user.application.port.in;

import com.msashop.user.application.port.in.model.ProvisionUserProfileCommand;

public interface ProvisionUserProfileUseCase {
    void provision(ProvisionUserProfileCommand command);
}
