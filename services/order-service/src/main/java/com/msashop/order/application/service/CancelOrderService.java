package com.msashop.order.application.service;

import com.msashop.order.application.port.in.CancelOrderUseCase;
import com.msashop.order.application.port.in.model.CancelOrderCommand;
import com.msashop.order.application.port.out.LoadOrderPort;
import com.msashop.order.application.port.out.SaveOrderPort;
import com.msashop.order.application.port.out.SaveOrderStatusHistoryPort;
import com.msashop.order.domain.model.Order;
import com.msashop.order.domain.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CancelOrderService implements CancelOrderUseCase {

    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;
    private final SaveOrderStatusHistoryPort saveOrderStatusHistoryPort;

    @Override
    public void cancelOrder(CancelOrderCommand command) {
        Order order = loadOrderPort.loadOrder(command.orderId());
        OrderStatus from = order.getStatus();
        order.cancel();
        saveOrderPort.save(order);
        saveOrderStatusHistoryPort.saveHistory(order.getOrderId(), from, order.getStatus(), command.reason(), command.userId());
    }
}

