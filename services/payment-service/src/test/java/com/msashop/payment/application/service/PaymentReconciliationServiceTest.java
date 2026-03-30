package com.msashop.payment.application.service;

import com.msashop.payment.application.port.out.GetPaymentStatusGatewayPort;
import com.msashop.payment.application.port.out.LoadPaymentPort;
import com.msashop.payment.application.port.out.model.PaymentGatewayStatusResult;
import com.msashop.payment.domain.model.PaymentStatus;
import com.msashop.payment.domain.model.PaymentTransaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReconciliationServiceTest {

    @Mock
    private LoadPaymentPort loadPaymentPort;

    @Mock
    private GetPaymentStatusGatewayPort getPaymentStatusGatewayPort;

    @Mock
    private PaymentSagaLocalTxService paymentSagaLocalTxService;

    @InjectMocks
    private PaymentReconciliationService service;

    @Test
    void should_resolve_unknown_payment_as_approved() {
        PaymentTransaction unknown = payment(100L, PaymentStatus.APPROVAL_UNKNOWN, null, null);
        PaymentGatewayStatusResult status = new PaymentGatewayStatusResult(true, false, "pg-tx-recon-1", null, null);

        when(loadPaymentPort.findApprovalUnknown(20)).thenReturn(List.of(unknown));
        when(getPaymentStatusGatewayPort.getStatus("FAKE", "idem-1", null)).thenReturn(status);

        service.reconcile(20);

        verify(paymentSagaLocalTxService).reconcileApproved(unknown, status);
    }

    @Test
    void should_resolve_unknown_payment_as_failed() {
        PaymentTransaction unknown = payment(100L, PaymentStatus.APPROVAL_UNKNOWN, null, null);
        PaymentGatewayStatusResult status = new PaymentGatewayStatusResult(false, true, null, "PG_RECON_FAILED", "forced failure");

        when(loadPaymentPort.findApprovalUnknown(20)).thenReturn(List.of(unknown));
        when(getPaymentStatusGatewayPort.getStatus("FAKE", "idem-1", null)).thenReturn(status);

        service.reconcile(20);

        verify(paymentSagaLocalTxService).reconcileFailed(unknown, status);
    }

    private PaymentTransaction payment(
            Long paymentId,
            PaymentStatus status,
            String providerTxId,
            String failReason
    ) {
        return PaymentTransaction.rehydrate(
                paymentId,
                1L,
                1L,
                new BigDecimal("10000"),
                "KRW",
                "idem-1",
                "FAKE",
                providerTxId,
                "reservation-1",
                "saga-1",
                "corr-1",
                "event-0",
                status,
                Instant.now(),
                null,
                null,
                failReason,
                Instant.now(),
                Instant.now()
        );
    }
}
