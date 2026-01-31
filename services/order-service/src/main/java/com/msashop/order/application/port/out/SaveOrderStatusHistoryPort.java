package com.msashop.order.application.port.out;

import com.msashop.order.domain.model.OrderStatus;

public interface SaveOrderStatusHistoryPort {
    void saveHistory(Long orderId, OrderStatus fromStatus, OrderStatus toStatus, String reason, Long changedBy);
}

