package com.msashop.order.adapter.out.persistence.adapter;

import com.msashop.order.adapter.out.persistence.entity.OrderEntity;
import com.msashop.order.adapter.out.persistence.entity.OrderStatusHistoryEntity;
import com.msashop.order.adapter.out.persistence.mapper.OrderEntityMapper;
import com.msashop.order.adapter.out.persistence.repo.OrderCommandJpaRepository;
import com.msashop.order.adapter.out.persistence.repo.OrderNumberRepository;
import com.msashop.order.adapter.out.persistence.repo.OrderQueryJpaRepository;
import com.msashop.order.adapter.out.persistence.repo.OrderStatusHistoryJpaRepository;
import com.msashop.order.application.port.out.LoadOrderPort;
import com.msashop.order.application.port.out.OrderNumberPort;
import com.msashop.order.application.port.out.SaveOrderPort;
import com.msashop.order.application.port.out.SaveOrderStatusHistoryPort;
import com.msashop.order.domain.model.Order;
import com.msashop.order.domain.model.OrderStatus;
import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.common.web.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional
public class OrderPersistenceAdapter implements SaveOrderPort, LoadOrderPort, OrderNumberPort, SaveOrderStatusHistoryPort {

    private final OrderCommandJpaRepository orderCommandJpaRepository;
    private final OrderQueryJpaRepository orderQueryJpaRepository;
    private final OrderNumberRepository orderNumberRepository;
    private final OrderStatusHistoryJpaRepository orderStatusHistoryJpaRepository;

    @Override
    public Long save(Order order) {
        OrderEntity entity = OrderEntityMapper.toEntity(order);
        OrderEntity saved = orderCommandJpaRepository.save(entity);
        return saved.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Order loadOrder(Long orderId) {
        OrderEntity entity = orderQueryJpaRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new NotFoundException(CommonErrorCode.COMMON_NOT_FOUND, "order not found: " + orderId));
        return OrderEntityMapper.toDomain(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> loadOrdersByUser(Long userId) {
        return orderQueryJpaRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderEntityMapper::toDomain)
                .toList();
    }

    @Override
    public String nextOrderNumber() {
        return orderNumberRepository.nextOrderNumber();
    }

    @Override
    public void saveHistory(Long orderId, OrderStatus fromStatus, OrderStatus toStatus, String reason, Long changedBy) {
        OrderStatusHistoryEntity history = OrderStatusHistoryEntity.builder()
                .orderId(orderId)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .reason(reason)
                .changedBy(changedBy)
                .build();
        orderStatusHistoryJpaRepository.save(history);
    }
}

