package com.msashop.order.application.port.in;

import com.msashop.order.application.port.in.model.OrderResult;

public interface GetOrderUseCase {
    OrderResult getOrder(Long orderId);
}

