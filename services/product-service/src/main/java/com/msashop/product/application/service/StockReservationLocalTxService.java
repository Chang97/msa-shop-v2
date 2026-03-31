package com.msashop.product.application.service;

import com.msashop.common.event.payload.StockReservationItemPayload;
import com.msashop.product.application.port.out.StockReservationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 재고 예약을 별도 로컬 트랜잭션으로 분리해 예약 실패가 바깥 saga 트랜잭션까지
 * rollback-only 로 번지지 않도록 한다.
 */
@Service
@RequiredArgsConstructor
public class StockReservationLocalTxService {

    private final StockReservationPort stockReservationPort;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserve(String reservationId, Long orderId, List<StockReservationItemPayload> items) {
        stockReservationPort.reserve(reservationId, orderId, items);
    }
}
