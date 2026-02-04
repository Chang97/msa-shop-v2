package com.msashop.payment.application.service;

import com.msashop.payment.application.port.in.ApprovePaymentUseCase;
import com.msashop.payment.application.port.in.model.ApprovePaymentCommand;
import com.msashop.payment.application.port.in.model.PaymentResult;
import com.msashop.payment.application.port.out.LoadPaymentPort;
import com.msashop.payment.application.port.out.MarkOrderPaidPort;
import com.msashop.payment.application.port.out.RequestOrderPaymentPort;
import com.msashop.payment.application.port.out.SavePaymentPort;
import com.msashop.payment.domain.model.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ApprovePaymentService implements ApprovePaymentUseCase {

    private static final String MARK_PAID_REASON = "payment approved (mock)";

    private final LoadPaymentPort loadPaymentPort;
    private final SavePaymentPort savePaymentPort;
    private final RequestOrderPaymentPort requestOrderPaymentPort;
    private final MarkOrderPaidPort markOrderPaidPort;

    @Override
    public PaymentResult approve(ApprovePaymentCommand command) {
        requestOrderPaymentPort.startPayment(command.orderId(), command.currentUser());

        Optional<PaymentTransaction> existing = loadPaymentPort.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            PaymentTransaction payment = existing.get();
            if (payment.isApproved()) {
                markOrderPaidPort.markPaid(payment.getOrderId(), payment.getPaymentId(), payment.getIdempotencyKey(), MARK_PAID_REASON);
            }
            return new PaymentResult(payment.getPaymentId(), payment.getOrderId(), payment.getStatus());
        }

        PaymentTransaction payment = PaymentTransaction.approve(
                command.orderId(),
                command.currentUser().userId(),
                command.amount(),
                "KRW",
                command.idempotencyKey(),
                "FAKE",
                UUID.randomUUID().toString(),
                Instant.now()
        );

        PaymentTransaction saved = savePaymentPort.save(payment);
        markOrderPaidPort.markPaid(saved.getOrderId(), saved.getPaymentId(), saved.getIdempotencyKey(), MARK_PAID_REASON);

        return new PaymentResult(saved.getPaymentId(), saved.getOrderId(), saved.getStatus());
    }
}

