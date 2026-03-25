package com.msashop.product.application.port.out;

import com.msashop.common.event.payload.StockReservationItemPayload;

import java.util.List;
import java.util.Optional;

public interface StockReservationPort {
    Optional<String> findActiveReservationId(Long orderId);

    void reserve(String reservationId, Long orderId, List<StockReservationItemPayload> items);

    void confirm(String reservationId);

    void release(String reservationId);
}