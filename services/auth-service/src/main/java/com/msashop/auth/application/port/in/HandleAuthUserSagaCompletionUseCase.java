package com.msashop.auth.application.port.in;

import com.msashop.common.event.EventEnvelope;

/**
 * Kafka consumer가 auth-service 회원가입 saga 완료 이벤트를 처리할 때 호출하는 inbound port.
 */
public interface HandleAuthUserSagaCompletionUseCase {

    /**
     * 회원가입 saga 완료 이벤트를 처리한다.
     *
     * @return true면 listener는 이 메시지를 ack 해도 된다.
     */
    boolean handle(
            String consumerGroup,
            String workerId,
            long claimTimeoutSeconds,
            EventEnvelope envelope
    ) throws Exception;
}
