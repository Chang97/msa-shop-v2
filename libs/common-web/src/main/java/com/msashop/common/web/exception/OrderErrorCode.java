package com.msashop.common.web.exception;

/**
 * order-service에서 사용하는 주문 도메인 전용 에러 코드.
 */
public enum OrderErrorCode implements ErrorCode {

    ORDER_NOT_FOUND("ORD_404_001", 404, "주문을 찾을 수 없습니다."),
    ORDER_ACCESS_DENIED("ORD_403_001", 403, "해당 주문에 접근할 권한이 없습니다."),
    ORDER_PRODUCT_NOT_FOUND("ORD_404_002", 404, "요청한 상품 중 존재하지 않는 상품이 있습니다."),
    ORDER_PRODUCT_INACTIVE("ORD_409_001", 409, "비활성화된 상품이 포함되어 있습니다."),
    ORDER_PRODUCT_NOT_ON_SALE("ORD_409_002", 409, "판매 중이 아닌 상품이 포함되어 있습니다."),
    ORDER_PRODUCT_STOCK_SHORTAGE("ORD_409_003", 409, "재고가 부족한 상품이 포함되어 있습니다."),
    ORDER_PAYMENT_NOT_ALLOWED("ORD_409_004", 409, "현재 주문 상태에서는 결제를 시작할 수 없습니다."),
    ORDER_CANCEL_NOT_ALLOWED("ORD_409_005", 409, "현재 주문 상태에서는 취소할 수 없습니다.");

    private final String code;
    private final int status;
    private final String defaultMessage;

    OrderErrorCode(String code, int status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public int status() {
        return status;
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }
}
