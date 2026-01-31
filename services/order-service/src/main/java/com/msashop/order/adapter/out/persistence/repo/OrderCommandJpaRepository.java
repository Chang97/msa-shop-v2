package com.msashop.order.adapter.out.persistence.repo;

import com.msashop.order.adapter.out.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderCommandJpaRepository extends JpaRepository<OrderEntity, Long> {
}

