package com.msashop.payment.adapter.out.persistence.adapter;

import com.msashop.payment.adapter.out.persistence.mapper.PaymentTransactionMapper;
import com.msashop.payment.adapter.out.persistence.repo.PaymentCommandJpaRepository;
import com.msashop.payment.application.port.out.SavePaymentPort;
import com.msashop.payment.domain.model.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class PaymentCommandPersistenceAdapter implements SavePaymentPort {

    private final PaymentCommandJpaRepository paymentCommandJpaRepository;

    @Override
    public PaymentTransaction save(PaymentTransaction paymentTransaction) {
        var entity = PaymentTransactionMapper.toEntity(paymentTransaction);
        var saved = paymentCommandJpaRepository.save(entity);
        return PaymentTransactionMapper.toDomain(saved);
    }
}

