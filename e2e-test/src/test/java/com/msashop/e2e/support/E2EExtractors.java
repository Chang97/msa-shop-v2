package com.msashop.e2e.support;

import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;

import java.util.List;

/**
 * E2E 응답 필드 추출 공용 헬퍼.
 * 필드명이 다르면 실패 메시지를 보고 키를 맞춰 주세요.
 */
public final class E2EExtractors {

    private static final List<String> ACCESS_TOKEN_KEYS = List.of("accessToken", "token", "access_token");
    private static final List<String> PRODUCT_ID_KEYS = List.of("productId", "id", "product.id");
    private static final List<String> ORDER_ID_KEYS = List.of("orderId", "id", "order.id");
    private static final List<String> ORDER_STATUS_KEYS = List.of("status", "orderStatus", "state");

    private E2EExtractors() {
    }

    public static String accessToken(Response resp) {
        return requireString(resp, ACCESS_TOKEN_KEYS, "엑세스 토큰");
    }

    public static Long productId(Response resp) {
        return requireLong(resp, PRODUCT_ID_KEYS, "상품 ID");
    }

    public static Long orderId(Response resp) {
        return requireLong(resp, ORDER_ID_KEYS, "주문 ID");
    }

    public static String orderStatus(Response resp) {
        return requireString(resp, ORDER_STATUS_KEYS, "주문 상태");
    }

    private static String requireString(Response resp, List<String> keys, String description) {
        Assertions.assertNotNull(resp, "응답이 null이라 " + description + "을(를) 추출할 수 없습니다.");
        for (String key : keys) {
            String value = resp.jsonPath().getString(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return Assertions.fail("응답에 " + description + " 필드가 없습니다. 확인한 키: " + keys + ". TODO: 실제 API 필드명에 맞게 키를 조정해 주세요.");
    }

    private static Long requireLong(Response resp, List<String> keys, String description) {
        Assertions.assertNotNull(resp, "응답이 null이라 " + description + "을(를) 추출할 수 없습니다.");
        for (String key : keys) {
            Object value = resp.jsonPath().get(key);
            Long converted = toLong(value);
            if (converted != null) {
                return converted;
            }
        }
        return Assertions.fail("응답에 " + description + " 필드가 없습니다. 확인한 키: " + keys + ". TODO: 실제 API 필드명에 맞게 키를 조정해 주세요.");
    }

    private static Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                // continue to next key
            }
        }
        return null;
    }
}
