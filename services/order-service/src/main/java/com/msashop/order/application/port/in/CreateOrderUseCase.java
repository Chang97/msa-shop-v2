package com.msashop.order.application.port.in;

import com.msashop.order.application.port.in.model.CreateOrderCommand;

public interface CreateOrderUseCase {
    Long createOrder(CreateOrderCommand command);
}

