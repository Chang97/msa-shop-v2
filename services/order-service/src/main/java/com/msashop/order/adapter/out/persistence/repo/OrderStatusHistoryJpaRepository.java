package com.msashop.order.adapter.out.persistence.repo;

import com.msashop.order.adapter.out.persistence.entity.OrderStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryJpaRepository extends JpaRepository<OrderStatusHistoryEntity, Long> {
}

