package com.msashop.common.event;

public final class EventTopics {
    private EventTopics() {}

    /** auth-service와 user-service 간 사가 이벤트에 사용하는 토픽이다. */
    public static final String AUTH_USER_SAGA_V1 = "auth.user.saga.v1";
}
