package com.msashop.user.adapter.out.persistence.repo;

import com.msashop.user.adapter.out.persistence.entity.OutboxEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, Long> {
}
