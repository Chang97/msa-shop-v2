package com.msashop.auth.adapter.out.persistence.repo;

import com.msashop.auth.adapter.out.persistence.entity.ProcessedEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventJpaEntity, Long> {
    // 같은 consumer group이 같은 이벤트를 이미 처리했는지 확인한다.
    boolean existsByConsumerGroupAndEventId(String consumerGroup, String eventId);
}
