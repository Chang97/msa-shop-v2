package com.msashop.auth.application.port.in;

import com.msashop.auth.application.port.in.model.RefreshCommand;
import com.msashop.auth.application.port.in.model.RefreshResult;

public interface RefreshUseCase {
    RefreshResult refresh(RefreshCommand command);
}

