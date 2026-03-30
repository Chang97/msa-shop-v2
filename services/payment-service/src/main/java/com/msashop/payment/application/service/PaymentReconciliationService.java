package com.msashop.payment.application.service;

import com.msashop.payment.application.port.out.GetPaymentStatusGatewayPort;
import com.msashop.payment.application.port.out.LoadPaymentPort;
import com.msashop.payment.application.port.out.model.PaymentGatewayStatusResult;
import com.msashop.payment.domain.model.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentReconciliationService {

    private final LoadPaymentPort loadPaymentPort;
    private final GetPaymentStatusGatewayPort getPaymentStatusGatewayPort;
    private final PaymentSagaLocalTxService paymentSagaLocalTxService;

    public void reconcile(int batchSize) {
        List<PaymentTransaction> unknownPayments = loadPaymentPort.findApprovalUnknown(batchSize);

        for (PaymentTransaction payment : unknownPayments) {
            PaymentGatewayStatusResult status = getPaymentStatusGatewayPort.getStatus(
                    payment.getProvider(),
                    payment.getIdempotencyKey(),
                    payment.getProviderTxId()
            );

            if (status.approved()) {
                paymentSagaLocalTxService.reconcileApproved(payment, status);
                continue;
            }

            if (status.failed()) {
                paymentSagaLocalTxService.reconcileFailed(payment, status);
            }
        }
    }
}
