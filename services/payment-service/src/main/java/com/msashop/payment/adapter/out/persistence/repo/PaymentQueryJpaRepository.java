package com.msashop.payment.adapter.out.persistence.repo;

import com.msashop.payment.adapter.out.persistence.entity.PaymentTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentQueryJpaRepository extends JpaRepository<PaymentTransactionEntity, Long> {
    Optional<PaymentTransactionEntity> findByIdempotencyKey(String idempotencyKey);
}

