package com.msashop.order.application.port.in;

import com.msashop.order.application.port.in.model.OrderResult;

import java.util.List;

public interface GetOrdersUseCase {
    List<OrderResult> getOrdersByUser(Long userId);
}

