package com.msashop.user.adapter.out.persistence.repo;

import com.msashop.user.adapter.out.persistence.entity.OutboxEventJpaEntity;
import com.msashop.user.domain.model.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, Long> {
    // relay가 발생할 대기 이벤트를 오래된 순서대로 가져온다.
    List<OutboxEventJpaEntity> findTop100ByStatusOrderByOutboxEventIdAsc(OutboxStatus status);
}
