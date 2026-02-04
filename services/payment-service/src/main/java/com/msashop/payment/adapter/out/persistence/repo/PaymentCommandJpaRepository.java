package com.msashop.payment.adapter.out.persistence.repo;

import com.msashop.payment.adapter.out.persistence.entity.PaymentTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCommandJpaRepository extends JpaRepository<PaymentTransactionEntity, Long> {
}

