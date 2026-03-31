package com.msashop.common.event;

public final class EventTypes {
    private EventTypes() {}

    // auth-user saga 이벤트
    public static final String AUTH_USER_CREATED = "AuthUserCreated";
    public static final String USER_PROFILE_CREATED = "UserProfileCreated";
    public static final String USER_PROFILE_CREATION_FAILED = "UserProfileCreationFailed";
    public static final String USER_DEACTIVATED = "UserDeactivated";

    // 결제 관련 이벤트
    public static final String STOCK_RESERVATION_REQUESTED = "StockReservationRequested";
    public static final String STOCK_RESERVED = "StockReserved";
    public static final String STOCK_RESERVATION_FAILED = "StockReservationFailed";
    public static final String PAYMENT_APPROVED = "PaymentApproved";
    public static final String PAYMENT_FAILED = "PaymentFailed";
}
