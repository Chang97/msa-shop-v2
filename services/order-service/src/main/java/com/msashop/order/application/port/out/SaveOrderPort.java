package com.msashop.order.application.port.out;

import com.msashop.order.domain.model.Order;

public interface SaveOrderPort {
    Long save(Order order);
}

