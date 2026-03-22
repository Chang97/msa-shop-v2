package com.msashop.user.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.payload.AuthUserCreatedPayload;
import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.common.web.exception.ValidationException;
import com.msashop.user.application.event.UserSagaEventFactory;
import com.msashop.user.application.port.in.ProvisionUserProfileUseCase;
import com.msashop.user.application.port.out.OutboxEventPort;
import com.msashop.user.application.port.out.ProcessedEventPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.mockito.Mockito.*;

/**
 * user-service의 saga 시작 이벤트 consumer 테스트.
 * 이 테스트는 "프로필 생성에 실패했을 때 실패 이벤트를 만들어 outbox에 적재하는가"를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AuthUserSagaKafkaConsumerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProcessedEventPort processedEventPort;

    @Mock
    private ProvisionUserProfileUseCase provisionUserProfileUseCase;

    @Mock
    private OutboxEventPort outboxEventPort;

    @Mock
    private UserSagaEventFactory eventFactory;

    @Mock
    private Acknowledgment acknowledgment;

    private AuthUserSagaKafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AuthUserSagaKafkaConsumer(
                objectMapper,
                processedEventPort,
                provisionUserProfileUseCase,
                outboxEventPort,
                eventFactory
        );

        // @Value 주입 필드는 단위 테스트에서 직접 세팅한다.
        ReflectionTestUtils.setField(consumer, "consumerGroup", "user-service-auth-user-saga");
    }

    @Test
    void should_append_failure_event_when_profile_creation_fails() throws Exception {
        // given
        // auth-service가 보낸 saga 시작 이벤트 원본
        EventEnvelope sourceEvent = new EventEnvelope(
                "event-1",
                EventTypes.AUTH_USER_CREATED,
                "AUTH_USER",
                "1",
                "saga-1",
                "corr-1",
                null,
                "auth-service",
                "auth.user.saga.v1",
                "1",
                Instant.now(),
                "{\"authUserId\":1}"
        );

        // 원본 이벤트 안의 실제 비즈니스 payload
        AuthUserCreatedPayload payload = new AuthUserCreatedPayload(
                1L,
                "홍길동",
                "EMP-1",
                "사원",
                "010-1111-2222"
        );

        // user-service가 실패 시 새로 발행할 saga 실패 이벤트
        EventEnvelope failureEvent = new EventEnvelope(
                "event-2",
                EventTypes.USER_PROFILE_CREATION_FAILED,
                "USER_PROFILE",
                "1",
                "saga-1",
                "corr-1",
                "event-1",
                "user-service",
                "auth.user.saga.v1",
                "1",
                Instant.now(),
                "{\"authUserId\":1,\"reasonCode\":\"USER_PROFILE_PERSISTENCE_FAILED\",\"reasonMessage\":\"강제 실패 테스트\"}"
        );

        when(objectMapper.readValue("raw-message", EventEnvelope.class)).thenReturn(sourceEvent);
        when(objectMapper.readValue(sourceEvent.payloadJson(), AuthUserCreatedPayload.class)).thenReturn(payload);
        when(processedEventPort.exists("user-service-auth-user-saga", "event-1")).thenReturn(false);

        // 프로필 생성 use case가 비즈니스 예외를 던지는 상황을 강제로 만든다.
        doThrow(new ValidationException(CommonErrorCode.COMMON_VALIDATION, "강제 실패 테스트"))
                .when(provisionUserProfileUseCase)
                .provision(any());

        // consumer는 catch 블록에서 factory를 호출해 failure event를 만든 뒤 outbox에 적재한다.
        when(eventFactory.userProfileCreationFailed(
                sourceEvent,
                1L,
                "USER_PROFILE_PERSISTENCE_FAILED",
                "강제 실패 테스트"
        )).thenReturn(failureEvent);

        // when
        consumer.onMessage("raw-message", acknowledgment);

        // then
        // 성공 이벤트는 만들면 안 된다.
        verify(eventFactory, never()).userProfileCreated(any(), anyLong(), anyLong());

        // 실패 이벤트는 정확히 만들어져야 한다.
        verify(eventFactory).userProfileCreationFailed(
                sourceEvent,
                1L,
                "USER_PROFILE_PERSISTENCE_FAILED",
                "강제 실패 테스트"
        );

        // 만들어진 실패 이벤트를 outbox에 적재해야 relay가 이후 Kafka로 넘길 수 있다.
        verify(outboxEventPort).append(failureEvent);

        // 원본 이벤트는 처리 완료로 기록해야 중복 소비를 막을 수 있다.
        verify(processedEventPort).save("user-service-auth-user-saga", sourceEvent);

        // 비즈니스 실패는 사가 실패로 정상 종료하는 것이므로 ack 한다.
        verify(acknowledgment).acknowledge();
    }
}
