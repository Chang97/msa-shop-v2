package com.msashop.auth.adapter.out.persistence.repo;


import com.msashop.auth.adapter.out.persistence.entity.OutboxEventJpaEntity;
import com.msashop.auth.domain.model.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, Long> {
    // relay가 아직 발행하지 않은 이벤트를 오래된 순서대로 가져온다.
    List<OutboxEventJpaEntity> findTop100ByStatusOrderByOutboxEventIdAsc(OutboxStatus status);
}
