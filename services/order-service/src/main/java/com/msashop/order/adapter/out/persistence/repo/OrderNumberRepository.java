package com.msashop.order.adapter.out.persistence.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderNumberRepository extends JpaRepository<com.msashop.order.adapter.out.persistence.entity.OrderEntity, Long> {

    @Query(value = "select next_order_number()", nativeQuery = true)
    String nextOrderNumber();
}

