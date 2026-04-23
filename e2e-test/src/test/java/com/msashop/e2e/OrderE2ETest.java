package com.msashop.e2e;

import com.msashop.e2e.support.E2EClient;
import com.msashop.e2e.support.E2EExtractors;
import com.msashop.e2e.support.TestFixtures;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// 주문 생성 단계의 검증 범위와 상태 전이를 검증한다.
class OrderE2ETest {

    private final E2EClient client = new E2EClient();

    // 정상 주문 생성 시 CREATED 상태로 저장되는지 검증한다.
    @Test
    void create_order_created() {
        // 주문 생성 API 는 재고를 선점하지 않고 CREATED 상태의 주문만 만든다.
        String adminToken = client.loginAdminToken();
        Long productId = createProduct(adminToken, "E2E Order Product", 9900, 5);

        String userToken = client.loginUserToken();

        Response order = client.postJson(
                "/api/orders",
                userToken,
                TestFixtures.order(productId, new BigDecimal("9900"), 2)
        );
        assertEquals(200, order.statusCode());

        Long orderId = E2EExtractors.orderId(order);
        Response orderDetail = client.get("/api/orders/" + orderId, userToken);
        assertEquals(200, orderDetail.statusCode());
        assertEquals("CREATED", E2EExtractors.orderStatus(orderDetail));
    }

    // 비활성 상품 또는 판매 중지 상품은 주문 생성이 거절되는지 검증한다.
    @Test
    void disabled_or_not_on_sale_product_should_fail() {
        // 주문 생성 단계에서는 상품의 사용 여부와 판매 상태만 검증한다.
        Response products = client.get("/api/products", null);
        assertEquals(200, products.statusCode());

        List<Map<String, Object>> items = products.jsonPath().getList("");
        Map<String, Object> disabled = findProductByName(items, "E2E Disabled Product");
        Map<String, Object> stopped = findProductByName(items, "E2E Stopped Product");

        assertNotNull(disabled, "E2E Disabled Product 시드가 필요합니다.");
        assertNotNull(stopped, "E2E Stopped Product 시드가 필요합니다.");

        String userToken = client.loginUserToken();

        Response disabledOrder = client.postJson(
                "/api/orders",
                userToken,
                TestFixtures.order(
                        ((Number) disabled.get("productId")).longValue(),
                        toBigDecimal(disabled.get("price")),
                        1
                )
        );
        assertEquals(409, disabledOrder.statusCode());

        Response stoppedOrder = client.postJson(
                "/api/orders",
                userToken,
                TestFixtures.order(
                        ((Number) stopped.get("productId")).longValue(),
                        toBigDecimal(stopped.get("price")),
                        1
                )
        );
        assertEquals(409, stoppedOrder.statusCode());
    }

    private Long createProduct(String adminToken, String productName, int price, int stock) {
        Response product = client.postJson(
                "/api/products",
                adminToken,
                TestFixtures.product(productName, price, stock)
        );
        assertEquals(200, product.statusCode());
        return E2EExtractors.productId(product);
    }

    private Map<String, Object> findProductByName(List<Map<String, Object>> products, String name) {
        if (products == null) {
            return null;
        }
        return products.stream()
                .filter(product -> name.equals(product.get("productName")))
                .findFirst()
                .orElse(null);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        if (value instanceof String text && !text.isBlank()) {
            return new BigDecimal(text);
        }
        throw new IllegalArgumentException("가격 변환 실패: " + value);
    }
}
