package com.msashop.order.application.service;

import com.msashop.order.application.mapper.OrderQueryMapper;
import com.msashop.order.application.port.in.GetOrdersUseCase;
import com.msashop.order.application.port.in.model.OrderResult;
import com.msashop.order.application.port.out.LoadOrderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetOrdersService implements GetOrdersUseCase {

    private final LoadOrderPort loadOrderPort;

    @Override
    public List<OrderResult> getOrdersByUser(Long userId) {
        return loadOrderPort.loadOrdersByUser(userId).stream()
                .map(OrderQueryMapper::toResult)
                .toList();
    }
}

