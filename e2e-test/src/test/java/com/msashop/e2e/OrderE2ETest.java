package com.msashop.e2e;

import com.msashop.e2e.support.E2EClient;
import com.msashop.e2e.support.E2EExtractors;
import com.msashop.e2e.support.TestFixtures;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
public class OrderE2ETest {
    private final E2EClient client = new E2EClient();

    @Test
    public void create_order_created() {
        // 1) 주문 생성 테스트용 상품 추가
        // 1.1) 관리자 로그인
        Response admin = client.postJson(
                "/api/auth/login",
                null,
                TestFixtures.adminLogin()
        );

        assertEquals(200, admin.getStatusCode());
        String adminAccessToken = E2EExtractors.accessToken(admin);
        assertNotNull(adminAccessToken);
        assertFalse(adminAccessToken.isBlank());
        // 1.2) 상품 생성
        var createProductBody = TestFixtures.product("주문 테스트 상품", 9900, 5);
        Response product = client.postJson(
                "/api/products",
                adminAccessToken,
                createProductBody
        );
        assertEquals(200, product.getStatusCode());
        Long productId = E2EExtractors.productId(product);

        // 2) 주문
        // 2.1) 일반 사용자 로그인
        Response user = client.postJson(
                "/api/auth/login",
                null,
                TestFixtures.userLogin()
        );
        assertEquals(200, user.getStatusCode());
        String userAccessToken = E2EExtractors.accessToken(user);
        assertNotNull(userAccessToken);
        assertFalse(userAccessToken.isBlank());

        // 2.2) 신규 주문 생성 (상품 정보는 위와 동일하게 입력)
        Response order = client.postJson(
                "/api/orders",
                userAccessToken,
                TestFixtures.order(productId, new BigDecimal(9900), 2)
        );
        assertEquals(200, order.getStatusCode());
        Long orderId = E2EExtractors.orderId(order);
        // 2.3) 주문 상태 확인 CREATED
        Response orderDetail = client.get("/api/orders/" + orderId, userAccessToken);
        assertEquals(200, orderDetail.getStatusCode());
        String status = E2EExtractors.orderStatus(orderDetail);
        assertEquals("CREATED", status, "초기 주문 상태는 CREATED 여야 합니다.");
    }

    @Test
    public void insufficient_stock_should_fail_on_order_create() {
        // 1) 관리자 로그인
        Response admin = client.postJson("/api/auth/login", null, TestFixtures.adminLogin());
        assertEquals(200, admin.getStatusCode());
        String adminAccessToken = E2EExtractors.accessToken(admin);

        // 2) 재고 1개짜리 상품 생성
        var createProductBody = TestFixtures.product("E2E Insufficient Product", 9900, 1);
        Response product = client.postJson("/api/products", adminAccessToken, createProductBody);
        assertTrue(product.getStatusCode() == 200 || product.getStatusCode() == 201);
        Long productId = E2EExtractors.productId(product);

        // 3) 사용자 로그인
        Response user = client.postJson("/api/auth/login", null, TestFixtures.userLogin());
        assertEquals(200, user.getStatusCode());
        String userAccessToken = E2EExtractors.accessToken(user);

        // 4) 재고보다 큰 수량 주문 → 409
        Response order = client.postJson(
                "/api/orders",
                userAccessToken,
                TestFixtures.order(productId, new BigDecimal(9900), 2)
        );
        assertEquals(409, order.getStatusCode());
    }

    @Test
    public void disabled_or_not_on_sale_product_should_fail() {
        // 시드 상품 조회
        Response productsResp = client.get("/api/products", null);
        assertEquals(200, productsResp.getStatusCode());
        List<Map<String, Object>> products = productsResp.jsonPath().getList("");

        Map<String, Object> disabled = findProductByName(products, "E2E Disabled Product");
        Map<String, Object> stopped = findProductByName(products, "E2E Stopped Product");

        assertNotNull(disabled, "E2E Disabled Product 시드가 필요합니다.");
        assertNotNull(stopped, "E2E Stopped Product 시드가 필요합니다.");

        Long disabledId = ((Number) disabled.get("productId")).longValue();
        Long stoppedId = ((Number) stopped.get("productId")).longValue();
        BigDecimal disabledPrice = toBigDecimal(disabled.get("price"));
        BigDecimal stoppedPrice = toBigDecimal(stopped.get("price"));

        // 사용자 로그인
        Response user = client.postJson("/api/auth/login", null, TestFixtures.userLogin());
        assertEquals(200, user.getStatusCode());
        String userAccessToken = E2EExtractors.accessToken(user);

        // useYn=false 상품 주문 → 409
        Response order1 = client.postJson(
                "/api/orders",
                userAccessToken,
                TestFixtures.order(disabledId, disabledPrice, 1)
        );
        assertEquals(409, order1.getStatusCode());

        // status!=ON_SALE 상품 주문 → 409
        Response order2 = client.postJson(
                "/api/orders",
                userAccessToken,
                TestFixtures.order(stoppedId, stoppedPrice, 1)
        );
        assertEquals(409, order2.getStatusCode());
    }

    private Map<String, Object> findProductByName(List<Map<String, Object>> products, String name) {
        if (products == null) return null;
        return products.stream()
                .filter(p -> name.equals(p.get("productName")))
                .findFirst()
                .orElse(null);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return new BigDecimal(n.toString());
        if (value instanceof String s && !s.isBlank()) return new BigDecimal(s);
        throw new IllegalArgumentException("가격 변환 실패: " + value);
    }

}
