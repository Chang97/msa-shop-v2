package com.msashop.common.web.exception;

/**
 * 결제/재고 차감 관련 에러 코드.
 */
public enum PaymentErrorCode implements ErrorCode {
    PAYMENT_STOCK_SHORTAGE("PAY_409_STOCK", 409, "결제를 처리하기에 재고가 부족합니다."),
    PAYMENT_IDEMPOTENCY_MISSING("PAY_404_IDEMPOTENCY", 404, "멱등 키를 찾을 수 없습니다.");

    private final String code;
    private final int status;
    private final String defaultMessage;

    PaymentErrorCode(String code, int status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    @Override public String code() { return code; }
    @Override public int status() { return status; }
    @Override public String defaultMessage() { return defaultMessage; }
}
