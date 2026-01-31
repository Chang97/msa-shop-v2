package com.msashop.order.application.service;

import com.msashop.order.application.mapper.OrderCommandMapper;
import com.msashop.order.application.port.in.CreateOrderUseCase;
import com.msashop.order.application.port.in.model.CreateOrderCommand;
import com.msashop.order.application.port.out.OrderNumberPort;
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
public class CreateOrderService implements CreateOrderUseCase {

    private final OrderNumberPort orderNumberPort;
    private final SaveOrderPort saveOrderPort;
    private final SaveOrderStatusHistoryPort saveOrderStatusHistoryPort;

    @Override
    public Long createOrder(CreateOrderCommand command) {
        String orderNumber = orderNumberPort.nextOrderNumber();
        Order order = OrderCommandMapper.toDomain(orderNumber, command);
        Long orderId = saveOrderPort.save(order);
        saveOrderStatusHistoryPort.saveHistory(orderId, null, OrderStatus.CREATED, "ORDER_CREATED", command.userId());
        return orderId;
    }
}

