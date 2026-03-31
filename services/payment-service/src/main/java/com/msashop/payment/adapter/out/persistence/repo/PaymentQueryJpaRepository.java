package com.msashop.payment.adapter.out.persistence.repo;

import com.msashop.payment.adapter.out.persistence.entity.PaymentTransactionEntity;
import com.msashop.payment.domain.model.PaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentQueryJpaRepository extends JpaRepository<PaymentTransactionEntity, Long> {
    Optional<PaymentTransactionEntity> findByIdempotencyKey(String idempotencyKey);

    List<PaymentTransactionEntity> findByStatusOrderByRequestedAtAsc(PaymentStatus status, Pageable pageable);
}
