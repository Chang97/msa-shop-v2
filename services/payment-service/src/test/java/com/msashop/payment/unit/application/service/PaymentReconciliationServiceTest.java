package com.msashop.payment.unit.application.service;

import com.msashop.payment.application.port.out.GetPaymentStatusGatewayPort;
import com.msashop.payment.application.port.out.LoadPaymentPort;
import com.msashop.payment.application.port.out.model.PaymentGatewayStatusResult;
import com.msashop.payment.application.service.PaymentReconciliationService;
import com.msashop.payment.application.service.PaymentSagaLocalTxService;
import com.msashop.payment.domain.model.PaymentStatus;
import com.msashop.payment.domain.model.PaymentTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.time.Instant;
import java.math.BigDecimal;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

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
    @DisplayName("APPROVAL_UNKNOWN가 승인으로 확인되면 승인 보정 처리를 수행한다")
    void should_resolve_unknown_payment_as_approved() {
        PaymentTransaction unknown = PaymentTestFixtures.payment(PaymentStatus.APPROVAL_UNKNOWN);
        PaymentGatewayStatusResult status = new PaymentGatewayStatusResult(true, false, "pg-tx-recon-1", null, null);

        when(loadPaymentPort.findApprovalUnknown(20)).thenReturn(List.of(unknown));
        when(getPaymentStatusGatewayPort.getStatus("FAKE", "idem-1", null)).thenReturn(status);

        service.reconcile(20);

        verify(paymentSagaLocalTxService).reconcileApproved(unknown, status);
    }

    @Test
    @DisplayName("APPROVAL_UNKNOWN가 실패로 확인되면 실패 보정 처리를 수행한다")
    void should_resolve_unknown_payment_as_failed() {
        PaymentTransaction unknown = PaymentTestFixtures.payment(PaymentStatus.APPROVAL_UNKNOWN);
        PaymentGatewayStatusResult status = new PaymentGatewayStatusResult(false, true, null, "PG_RECON_FAILED", "forced failure");

        when(loadPaymentPort.findApprovalUnknown(20)).thenReturn(List.of(unknown));
        when(getPaymentStatusGatewayPort.getStatus("FAKE", "idem-1", null)).thenReturn(status);

        service.reconcile(20);

        verify(paymentSagaLocalTxService).reconcileFailed(unknown, status);
    }

    @Test
    @DisplayName("승인도 실패도 아닌 상태면 아무 보정도 하지 않는다")
    void should_do_nothing_when_gateway_status_is_still_unknown() {
        PaymentTransaction unknown = PaymentTestFixtures.payment(PaymentStatus.APPROVAL_UNKNOWN);
        PaymentGatewayStatusResult status = new PaymentGatewayStatusResult(false, false, null, null, null);

        when(loadPaymentPort.findApprovalUnknown(20)).thenReturn(List.of(unknown));
        when(getPaymentStatusGatewayPort.getStatus("FAKE", "idem-1", null)).thenReturn(status);

        service.reconcile(20);

        verify(paymentSagaLocalTxService, never()).reconcileApproved(unknown, status);
        verify(paymentSagaLocalTxService, never()).reconcileFailed(unknown, status);
    }

    @Test
    @DisplayName("한 건 reconciliation 실패가 나도 다음 결제는 계속 처리한다")
    void should_continue_reconciliation_when_one_payment_fails() {
        PaymentTransaction first = PaymentTestFixtures.payment(PaymentStatus.APPROVAL_UNKNOWN);
        PaymentTransaction second = PaymentTransaction.rehydrate(
                2L,
                2L,
                1L,
                new BigDecimal("10000"),
                "KRW",
                "idem-2",
                "FAKE",
                null,
                "reservation-2",
                "saga-2",
                "corr-2",
                "event-2",
                PaymentStatus.APPROVAL_UNKNOWN,
                Instant.now(),
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );
        PaymentGatewayStatusResult approved = new PaymentGatewayStatusResult(true, false, "pg-tx-2", null, null);

        when(loadPaymentPort.findApprovalUnknown(20)).thenReturn(List.of(first, second));
        when(getPaymentStatusGatewayPort.getStatus("FAKE", "idem-1", null)).thenThrow(new RuntimeException("status timeout"));
        when(getPaymentStatusGatewayPort.getStatus("FAKE", "idem-2", null)).thenReturn(approved);

        service.reconcile(20);

        verify(paymentSagaLocalTxService, never()).reconcileApproved(first, approved);
        verify(paymentSagaLocalTxService, times(1)).reconcileApproved(second, approved);
    }
}
