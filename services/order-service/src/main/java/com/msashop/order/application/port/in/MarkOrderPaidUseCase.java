package com.msashop.order.application.port.in;

import com.msashop.order.application.port.in.model.MarkOrderPaidCommand;

public interface MarkOrderPaidUseCase {
    void markPaid(MarkOrderPaidCommand command);
}

