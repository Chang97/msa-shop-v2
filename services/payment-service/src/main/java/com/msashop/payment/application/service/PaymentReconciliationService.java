package com.msashop.payment.application.service;

import com.msashop.payment.application.event.PaymentSagaEventFactory;
import com.msashop.payment.application.port.out.GetPaymentStatusGatewayPort;
import com.msashop.payment.application.port.out.LoadPaymentPort;
import com.msashop.payment.application.port.out.OutboxEventPort;
import com.msashop.payment.application.port.out.SavePaymentPort;
import com.msashop.payment.application.port.out.model.PaymentGatewayStatusResult;
import com.msashop.payment.domain.model.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentReconciliationService {

    private final LoadPaymentPort loadPaymentPort;
    private final SavePaymentPort savePaymentPort;
    private final GetPaymentStatusGatewayPort getPaymentStatusGatewayPort;
    private final OutboxEventPort outboxEventPort;
    private final PaymentSagaEventFactory paymentSagaEventFactory;

    @Transactional
    public void reconcile(int batchSize) {
        // 여기서는 APPROVAL_UNKNOWN 상태만 다시 본다.
        // APPROVED, FAILED는 이미 최종 saga 결과를 발행한 상태다.
        List<PaymentTransaction> unknownPayments = loadPaymentPort.findApprovalUnknown(batchSize);

        for (PaymentTransaction payment : unknownPayments) {
            PaymentGatewayStatusResult status = getPaymentStatusGatewayPort.getStatus(
                    payment.getProvider(),
                    payment.getIdempotencyKey(),
                    payment.getProviderTxId()
            );

            if (status.approved()) {
                PaymentTransaction approved = savePaymentPort.save(
                        payment.markApproved(status.providerTxId(), Instant.now())
                );

                // 복구 경로라도 최종 이벤트 형태는 정상 경로와 동일하게 맞춘다.
                outboxEventPort.append(
                        paymentSagaEventFactory.paymentApprovedByReconciliation(approved)
                );
                continue;
            }

            if (status.failed()) {
                PaymentTransaction failed = savePaymentPort.save(
                        payment.markFailed(status.reasonMessage(), Instant.now())
                );

                outboxEventPort.append(
                        paymentSagaEventFactory.paymentFailedByReconciliation(
                                failed,
                                status.reasonCode(),
                                status.reasonMessage()
                        )
                );
            }
        }
    }
}
