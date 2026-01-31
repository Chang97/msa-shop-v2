package com.msashop.order.application.port.in;

import com.msashop.order.application.port.in.model.CancelOrderCommand;

public interface CancelOrderUseCase {
    void cancelOrder(CancelOrderCommand command);
}

