package com.msashop.product.adapter.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 만료된 RESERVED 예약을 주기적으로 회수하는 스케줄러다.
 */
@Component
@RequiredArgsConstructor
public class ExpiredStockReservationScheduler {

    private final StockReservationPersistenceAdapter stockReservationPersistenceAdapter;

    @Value("${app.stock-reservation.expire-enabled:true}")
    private boolean expireEnabled;

    @Scheduled(fixedDelayString = "${app.stock-reservation.expire-fixed-delay-ms:60000}")
    public void expireReservations() {
        if (!expireEnabled) {
            return;
        }
        stockReservationPersistenceAdapter.expireReservations(Instant.now());
    }
}
