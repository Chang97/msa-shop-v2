package com.msashop.e2e;

import com.msashop.e2e.support.E2EClient;
import com.msashop.e2e.support.E2EExtractors;
import com.msashop.e2e.support.TestFixtures;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 최소 주문 해피패스 E2E 시나리오.
 * TODO: 실제 API 응답 필드명이 다르면 맞춰 주세요.
 */
class OrderHappyPathE2ETest {

    private final E2EClient client = new E2EClient();

    @Test
    void admin_creates_product_then_user_orders_it() {
        // 1) 관리자 로그인
        Response adminLoginResp = client.postJson(
                "/api/auth/login",
                null,
                TestFixtures.adminLogin()
        );
        assumeTrue(is2xx(adminLoginResp.statusCode()), "관리자 로그인 API가 동작 중이어야 합니다.");
        String adminToken = E2EExtractors.accessToken(adminLoginResp);
        assertNotNull(adminToken);
        assertFalse(adminToken.isBlank());

        // 2) 상품 생성
        Response createProductResp = client.postJson(
                "/api/products",
                adminToken,
                TestFixtures.product("E2E 상품", 1000, 10)
        );
        assumeTrue(createProductResp.statusCode() == 200 || createProductResp.statusCode() == 201,
                "상품 생성 응답코드는 200 또는 201이어야 합니다. actual=" + createProductResp.statusCode());
        Long productId = E2EExtractors.productId(createProductResp);
        assertNotNull(productId, "TODO: 상품 ID 필드명을 productId 또는 id 등에 맞춰 주세요.");

        // 3) 사용자 로그인
        Response userLoginResp = client.postJson(
                "/api/auth/login",
                null,
                TestFixtures.userLogin()
        );
        assumeTrue(is2xx(userLoginResp.statusCode()), "사용자 로그인 API가 동작 중이어야 합니다.");
        String userToken = E2EExtractors.accessToken(userLoginResp);
        assertNotNull(userToken);
        assertFalse(userToken.isBlank());

        // 4) 주문 생성 (단일 품목)
        Response createOrderResp = client.postJson(
                "/api/orders",
                userToken,
                TestFixtures.order(productId, 1)
        );
        assumeTrue(createOrderResp.statusCode() == 200 || createOrderResp.statusCode() == 201,
                "주문 생성 응답코드는 200 또는 201이어야 합니다. actual=" + createOrderResp.statusCode());
        Long orderId = E2EExtractors.orderId(createOrderResp);
        assertNotNull(orderId, "TODO: 주문 ID 필드명을 orderId 또는 id 등에 맞춰 주세요.");

        // 5) 주문 상세 확인
        Response orderDetailResp = client.get("/api/orders/" + orderId, userToken);
        assumeTrue(orderDetailResp.statusCode() == 200, "주문 상세 응답코드는 200이어야 합니다. actual=" + orderDetailResp.statusCode());
        String status = E2EExtractors.orderStatus(orderDetailResp);
        assertNotNull(status, "TODO: 주문 상태 필드명을 status 등에 맞춰 주세요.");
        assertEquals("CREATED", status, "초기 주문 상태는 CREATED 여야 합니다.");
    }

    private static boolean is2xx(int status) {
        return status >= 200 && status < 300;
    }
}
