package com.msashop.order.application.service;

import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.common.web.exception.ConflictException;
import com.msashop.order.application.port.in.MarkOrderPaidUseCase;
import com.msashop.order.application.port.in.model.MarkOrderPaidCommand;
import com.msashop.order.application.port.out.DecreaseProductStockPort;
import com.msashop.order.application.port.out.LoadOrderPort;
import com.msashop.order.application.port.out.SaveOrderPort;
import com.msashop.order.application.port.out.SaveOrderStatusHistoryPort;
import com.msashop.order.domain.model.Order;
import com.msashop.order.domain.model.OrderItem;
import com.msashop.order.domain.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MarkOrderPaidService implements MarkOrderPaidUseCase {

    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;
    private final SaveOrderStatusHistoryPort saveOrderStatusHistoryPort;
    private final DecreaseProductStockPort decreaseProductStockPort;

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
            decreaseProductStockPort.decreaseStocks(buildQuantities(order));
            saveOrderPort.save(order);
            saveOrderStatusHistoryPort.saveHistory(order.getOrderId(), from, order.getStatus(), command.reason(), order.getUserId());
        }
    }

    private Map<Long, Integer> buildQuantities(Order order) {
        return order.getItems().stream()
                .collect(Collectors.toMap(
                        OrderItem::getProductId,
                        OrderItem::getQuantity,
                        Integer::sum
                ));
    }
}
