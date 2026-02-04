package com.msashop.order.application.service;

import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.common.web.exception.ConflictException;
import com.msashop.order.application.port.in.MarkOrderPaidUseCase;
import com.msashop.order.application.port.in.model.MarkOrderPaidCommand;
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
public class MarkOrderPaidService implements MarkOrderPaidUseCase {

    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;
    private final SaveOrderStatusHistoryPort saveOrderStatusHistoryPort;

    @Override
    public void markPaid(MarkOrderPaidCommand command) {
        Order order = loadOrderPort.loadOrder(command.orderId());
        OrderStatus from = order.getStatus();
        try {
            order.markPaid();
        } catch (IllegalStateException e) {
            throw new ConflictException(CommonErrorCode.COMMON_CONFLICT, e.getMessage());
        }

        if (from != order.getStatus()) {
            saveOrderPort.save(order);
            saveOrderStatusHistoryPort.saveHistory(order.getOrderId(), from, order.getStatus(), command.reason(), order.getUserId());
        }
    }
}
