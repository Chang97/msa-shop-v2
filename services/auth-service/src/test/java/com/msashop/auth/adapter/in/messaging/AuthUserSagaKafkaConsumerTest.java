package com.msashop.auth.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.auth.application.port.out.CredentialPort;
import com.msashop.auth.application.port.out.ProcessedEventPort;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.*;

/**
 * auth-service의 saga 완료 consumer 테스트.
 * 이 테스트는 "실패 이벤트를 받았을 때 계정을 활성화하지 않고 disabled 상태를 유지하는가"를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AuthUserSagaKafkaConsumerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProcessedEventPort processedEventPort;

    @Mock
    private CredentialPort credentialPort;

    @Mock
    private Acknowledgment acknowledgment;

    private AuthUserSagaKafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AuthUserSagaKafkaConsumer(objectMapper, processedEventPort, credentialPort);

        // @Value로 주입되는 consumer group은 단위 테스트에서 직접 넣어준다.
        ReflectionTestUtils.setField(consumer, "consumerGroup", "auth-service-auth-user-saga");
    }

    @Test
    void should_keep_disabled_when_user_profile_creation_failed_event_is_received() throws Exception {
        // given
        // auth-service가 받아야 하는 실패 완료 이벤트를 준비한다.
        EventEnvelope failureEvent = new EventEnvelope(
                "event-1",
                EventTypes.USER_PROFILE_CREATION_FAILED,
                "USER_PROFILE",
                "1",
                "saga-1",
                "corr-1",
                "cause-1",
                "user-service",
                "auth.user.saga.v1",
                "1",
                Instant.now(),
                "{\"authUserId\":1,\"reasonCode\":\"USER_PROFILE_PERSISTENCE_FAILED\",\"reasonMessage\":\"강제 실패\"}"
        );

        when(objectMapper.readValue("raw-message", EventEnvelope.class)).thenReturn(failureEvent);
        when(processedEventPort.exists("auth-service-auth-user-saga", "event-1")).thenReturn(false);

        // when
        consumer.onMessage("raw-message", acknowledgment);

        // then
        // 실패 보상 전략은 delete가 아니라 disabled 유지이므로 enable()이 호출되면 안 된다.
        verify(credentialPort, never()).enable(anyLong());

        // 실패 이벤트도 정상적으로 처리한 이벤트이므로 processed_event는 남겨야 한다.
        verify(processedEventPort).save("auth-service-auth-user-saga", failureEvent);

        // 메시지는 처리 완료로 간주하므로 수동 ack를 호출해야 한다.
        verify(acknowledgment).acknowledge();
    }
}