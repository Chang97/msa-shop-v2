package com.msashop.order.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 일정 시간 이상 결제 진행 중으로 남은 주문을 만료 처리한다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.order.payment-expiration", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PendingPaymentExpirationScheduler {

    private final PendingPaymentExpirationService pendingPaymentExpirationService;

    @Value("${app.order.payment-expiration.timeout-seconds:900}")
    private long timeoutSeconds;

    @Value("${app.order.payment-expiration.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.order.payment-expiration.fixed-delay-ms:10000}")
    public void expirePendingPayments() {
        pendingPaymentExpirationService.expirePendingPayments(
                Instant.now().minusSeconds(timeoutSeconds),
                batchSize
        );
    }
}
