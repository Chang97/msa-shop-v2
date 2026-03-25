package com.msashop.common.event;

/**
 * 서비스별 DLQ publisher를 공통 listener support가 다룰 수 있게 맞추는 최소 포트.
 */
public interface DeadLetterPublisherPort {

    void publish(
            String dlqTopic,
            String originalTopic,
            String consumerGroup,
            String reasonCode,
            String reasonMessage,
            String originalMessage
    );
}
