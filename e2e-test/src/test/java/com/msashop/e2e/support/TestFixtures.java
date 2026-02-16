package com.msashop.e2e.support;

import java.math.BigDecimal;
import java.util.List;

/**
 * E2E 요청을 위한 공용 픽스처 빌더.
 * TODO: 실제 API 필드명이 다르면 맞춰 주세요.
 */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static LoginRequest adminLogin() {
        return new LoginRequest("admin", "1234");
    }

    public static LoginRequest userLogin() {
        return new LoginRequest("user1", "1234");
    }

    public static CreateProductRequest product(String name, int price, int stock) {
        String resolvedName = (name == null || name.isBlank()) ? "E2E 상품" : name;
        return new CreateProductRequest(resolvedName, price, stock);
    }

    public static CreateOrderRequest order(Long productId, BigDecimal price, int qty) {
        return new CreateOrderRequest(
                "KRW",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "테스터",
                "010-0000-0000",
                "00000",
                "서울시 테스트로 1",
                null,
                null,
                List.of(new OrderItemRequest(productId, "E2E 상품", price, qty))
        );
    }

    public static ApprovePaymentRequest approvePayment(Long orderId, BigDecimal amount, String idempotencyKey) {
        return new ApprovePaymentRequest(orderId, amount, idempotencyKey);
    }

    public static CancelOrderRequest cancel(String reason) {
        return new CancelOrderRequest(reason);
    }

    public static record LoginRequest(String loginId, String password) { }

    public static record CreateProductRequest(String productName, int price, int stock) { }

    public static record CreateOrderRequest(
            String currency,
            BigDecimal discountAmount,
            BigDecimal shippingFee,
            String receiverName,
            String receiverPhone,
            String shippingPostcode,
            String shippingAddress1,
            String shippingAddress2,
            String memo,
            List<OrderItemRequest> items
    ) { }

    public static record OrderItemRequest(Long productId, String productName, BigDecimal unitPrice, int quantity) { }

    public static record ApprovePaymentRequest(Long orderId, BigDecimal amount, String idempotencyKey) { }

    public static record CancelOrderRequest(String reason) { }
}
