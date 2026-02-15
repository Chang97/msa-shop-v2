package com.msashop.e2e;

import com.msashop.e2e.support.E2EClient;
import com.msashop.e2e.support.E2EExtractors;
import com.msashop.e2e.support.TestFixtures;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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
    public void approve_payment_transitions_to_paid_and_decreases_stock() {

    }

}
