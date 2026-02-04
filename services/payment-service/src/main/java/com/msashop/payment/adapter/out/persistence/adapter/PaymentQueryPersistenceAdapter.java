package com.msashop.payment.adapter.out.persistence.adapter;

import com.msashop.payment.adapter.out.persistence.mapper.PaymentTransactionMapper;
import com.msashop.payment.adapter.out.persistence.repo.PaymentQueryJpaRepository;
import com.msashop.payment.application.port.out.LoadPaymentPort;
import com.msashop.payment.domain.model.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentQueryPersistenceAdapter implements LoadPaymentPort {

    private final PaymentQueryJpaRepository paymentQueryJpaRepository;

    @Override
    public Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey) {
        return paymentQueryJpaRepository.findByIdempotencyKey(idempotencyKey)
                .map(PaymentTransactionMapper::toDomain);
    }
}

