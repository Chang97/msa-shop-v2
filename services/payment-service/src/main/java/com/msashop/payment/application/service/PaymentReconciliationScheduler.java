package com.msashop.payment.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * APPROVAL_UNKNOWN 결제를 주기적으로 확정하는 스케줄러.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.payment.reconciliation", name = "enabled", havingValue = "true")
public class PaymentReconciliationScheduler {

    private final PaymentReconciliationService paymentReconciliationService;

    @Value("${app.payment.reconciliation.batch-size:20}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.payment.reconciliation.fixed-delay-ms:5000}")
    public void reconcile() {
        paymentReconciliationService.reconcile(batchSize);
    }
}