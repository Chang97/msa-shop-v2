package com.msashop.product.adapter.out.persistence.repo;

import com.msashop.product.adapter.out.persistence.entity.StockReservationEntity;
import com.msashop.product.domain.model.StockReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockReservationJpaRepository extends JpaRepository<StockReservationEntity, Long> {

    List<StockReservationEntity> findByReservationId(String reservationId);

    Optional<StockReservationEntity> findFirstByOrderIdAndStatusIn(
            Long orderId,
            Collection<StockReservationStatus> statuses
    );
}