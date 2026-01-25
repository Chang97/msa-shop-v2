package com.msashop.auth.command.application.port.in;

import com.msashop.auth.command.application.port.in.model.RefreshCommand;
import com.msashop.auth.command.application.port.in.model.RefreshResult;

public interface RefreshUseCase {
    RefreshResult refresh(RefreshCommand command);
}
