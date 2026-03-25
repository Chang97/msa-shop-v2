package com.msashop.e2e.support;

import java.math.BigDecimal;
import java.util.List;

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
        String resolvedName = (name == null || name.isBlank()) ? "E2E Product" : name;
        return new CreateProductRequest(resolvedName, price, stock);
    }

    public static CreateOrderRequest order(Long productId, BigDecimal price, int qty) {
        return new CreateOrderRequest(
                "KRW",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "Tester",
                "010-0000-0000",
                "00000",
                "Seoul Test St 1",
                null,
                null,
                List.of(new OrderItemRequest(productId, "E2E Product", price, qty))
        );
    }

    public static PayOrderRequest pay(String idempotencyKey, String provider) {
        return new PayOrderRequest(idempotencyKey, provider);
    }

    public static CancelOrderRequest cancel(String reason) {
        return new CancelOrderRequest(reason);
    }

    public static RegisterRequest register(String suffix) {
        return new RegisterRequest(
                "e2e-" + suffix + "@example.com",
                "e2e-" + suffix,
                "1234",
                "E2E User " + suffix,
                "EMP-" + suffix,
                "QA",
                "010-1111-2222"
        );
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

    public static record PayOrderRequest(String idempotencyKey, String provider) { }

    public static record CancelOrderRequest(String reason) { }

    public static record RegisterRequest(
            String email,
            String loginId,
            String password,
            String userName,
            String empNo,
            String pstnName,
            String tel
    ) { }
}
