package com.msashop.e2e.support;

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

    public static CreateOrderRequest order(Long productId, int qty) {
        return new CreateOrderRequest(List.of(new OrderItemRequest(productId, qty)));
    }

    public static record LoginRequest(String loginId, String password) { }

    public static record CreateProductRequest(String productName, int price, int stock) { }

    public static record CreateOrderRequest(List<OrderItemRequest> items) { }

    public static record OrderItemRequest(Long productId, int quantity) { }
}
