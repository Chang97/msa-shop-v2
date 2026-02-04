package com.msashop.order.application.port.in;

import com.msashop.order.application.port.in.model.PayOrderCommand;

public interface PayOrderUseCase {
    void payOrder(PayOrderCommand command);
}

