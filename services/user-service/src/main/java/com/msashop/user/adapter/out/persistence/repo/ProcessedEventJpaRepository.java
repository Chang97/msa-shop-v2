package com.msashop.user.adapter.out.persistence.repo;

import com.msashop.user.adapter.out.persistence.entity.ProcessedEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventJpaEntity, Long> {
    // 같은 consumer group이 같은 eventId를 처리했는지 확인한다.
    boolean existsByConsumerGroupAndEventId(String consumerGroup, String eventId);
}
