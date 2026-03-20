package com.msashop.payment.application.service;

import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.common.web.exception.NotFoundException;
import com.msashop.common.web.exception.PaymentErrorCode;
import com.msashop.payment.application.port.in.ApprovePaymentUseCase;
import com.msashop.payment.application.port.in.model.ApprovePaymentCommand;
import com.msashop.payment.application.port.in.model.PaymentResult;
import com.msashop.payment.application.port.out.*;
import com.msashop.payment.config.redis.IdempotencyProperties;
import com.msashop.payment.domain.model.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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
    private final IdempotencyProperties idempotencyProps;
    private final IdempotencyPort idempotencyPort;


    @Override
    public PaymentResult approve(ApprovePaymentCommand command) {
        Duration ttl = Duration.ofSeconds(idempotencyProps.ttlSeconds());

        // 1) Redis 선행 차단
        if (!idempotencyPort.tryAcquire(command.idempotencyKey(), ttl)) {
            // 이미 진행/완료된 요청 → DB에서 조회 후 그대로 반환(멱등 OK)
            return loadPaymentPort.findByIdempotencyKey(command.idempotencyKey())
                    .map(this::toResult)
                    // 없으면 정책에 따라 409나 예외
                    .orElseThrow(() -> new NotFoundException(PaymentErrorCode.PAYMENT_IDEMPOTENCY_MISSING));
        }

        try {
            // 2) 기존 DB 멱등 세이프티넷 유지
            PaymentTransaction payment = loadPaymentPort.findByIdempotencyKey(command.idempotencyKey())
                    .orElseGet(() -> createApprovedPayment(command));

            if (payment.isApproved()) {
                markOrderPaidPort.markPaid(
                        payment.getOrderId(), payment.getPaymentId(),
                        payment.getIdempotencyKey(), MARK_PAID_REASON
                );
            }
            return toResult(payment);

        } catch (DataIntegrityViolationException e) {
            // DB UNIQUE 충돌 → 이미 존재하는 경우 멱등 OK로 처리
            return loadPaymentPort.findByIdempotencyKey(command.idempotencyKey())
                    .map(this::toResult)
                    .orElseThrow(() -> new NotFoundException(PaymentErrorCode.PAYMENT_IDEMPOTENCY_MISSING));
        } catch (RuntimeException e) {
            // 비즈니스 실패 시 Redis 키 정리(정책에 따라 생략 가능)
            idempotencyPort.release(command.idempotencyKey());
            throw e;
        }
        // 성공 시에는 TTL 자연 만료에 맡김
    }

    private PaymentTransaction createApprovedPayment(ApprovePaymentCommand command) {
        requestOrderPaymentPort.startPayment(command.orderId(), command.currentUser());

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

        try {
            return savePaymentPort.save(payment);
        } catch (DataIntegrityViolationException e) {
            Optional<PaymentTransaction> existing = loadPaymentPort.findByIdempotencyKey(command.idempotencyKey());
            return existing.orElseThrow(() -> e);
        }
    }

    private PaymentResult toResult(PaymentTransaction payment) {
        return new PaymentResult(payment.getPaymentId(), payment.getOrderId(), payment.getStatus());
    }
}
