package com.msashop.common.event;

public final class EventTopics {
    private EventTopics() {
    }
    // 회원가입
    public static final String AUTH_USER_SAGA_V1 = "auth.user.saga.v1";
    public static final String AUTH_USER_SAGA_V1_DLQ = "auth.user.saga.v1.dlq";
    // 결제
    public static final String ORDER_PAYMENT_SAGA_V1 = "order.payment.saga.v1";
    public static final String ORDER_PAYMENT_SAGA_V1_DLQ = "order.payment.saga.v1.dlq";
}
