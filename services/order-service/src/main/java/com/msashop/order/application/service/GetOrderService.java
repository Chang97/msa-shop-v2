package com.msashop.order.application.service;

import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.OrderErrorCode;
import com.msashop.order.application.mapper.OrderQueryMapper;
import com.msashop.order.application.port.in.GetOrderUseCase;
import com.msashop.order.application.port.in.model.OrderResult;
import com.msashop.order.application.port.out.LoadOrderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetOrderService implements GetOrderUseCase {

    private final LoadOrderPort loadOrderPort;

    @Override
    public OrderResult getOrder(Long orderId, Long userId) {
        var order = loadOrderPort.loadOrder(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }
        return OrderQueryMapper.toResult(order);
    }
}
