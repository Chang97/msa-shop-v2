package com.msashop.order.adapter.out.persistence.repo;

import com.msashop.order.adapter.out.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderQueryJpaRepository extends JpaRepository<OrderEntity, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<OrderEntity> findWithItemsById(Long id);

    @EntityGraph(attributePaths = "items")
    List<OrderEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}

