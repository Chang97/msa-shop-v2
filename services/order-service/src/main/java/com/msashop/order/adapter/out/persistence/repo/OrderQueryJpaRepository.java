package com.msashop.order.adapter.out.persistence.repo;

import com.msashop.order.adapter.out.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderQueryJpaRepository extends JpaRepository<OrderEntity, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<OrderEntity> findWithItemsById(Long id);

    @EntityGraph(attributePaths = "items")
    List<OrderEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
            select o.id
            from OrderEntity o
            where o.status = com.msashop.order.domain.model.OrderStatus.PENDING_PAYMENT
              and o.updatedAt < :threshold
            order by o.updatedAt asc
            """)
    List<Long> findPendingPaymentOrderIdsBefore(@Param("threshold") Instant threshold, org.springframework.data.domain.Pageable pageable);
}
