package com.msashop.order.application.port.out;

import com.msashop.order.domain.model.Order;

import java.util.List;

public interface LoadOrderPort {
    Order loadOrder(Long orderId);
    List<Order> loadOrdersByUser(Long userId);
}

