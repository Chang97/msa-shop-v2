package com.msashop.e2e;

import com.msashop.e2e.support.E2EClient;
import com.msashop.e2e.support.E2EExtractors;
import com.msashop.e2e.support.TestFixtures;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// 상품 조회 공개 범위와 생성 권한을 검증한다.
public class ProductE2ETest {
    private final E2EClient e2EClient = new E2EClient();

    // 비로그인 사용자도 상품 목록을 조회할 수 있는지 검증한다.
    @Test
    void public_can_get_product_list() {
        Response listResp = e2EClient.get("/api/products", null);
        assertEquals(200, listResp.getStatusCode());

        var items = listResp.jsonPath().getList("$");
        assertNotNull(items);
    }

    // 상품 생성은 관리자만 가능하고, 생성된 상품은 공개 조회가 가능한지 검증한다.
    @Test
    public void only_admin_can_create_product() {
        // 1) 일반 사용자는 상품 생성에 실패해야 한다.
        Response user = e2EClient.postJson(
                "/api/auth/login",
                null,
                TestFixtures.userLogin()
        );

        assertEquals(200, user.getStatusCode());
        String userAccessToken = E2EExtractors.accessToken(user);
        assertNotNull(userAccessToken);
        assertFalse(userAccessToken.isBlank());

        var createProductBody = TestFixtures.product("테스트 상품", 9900, 5);
        Response product1 = e2EClient.postJson(
                "/api/products",
                userAccessToken,
                createProductBody
        );
        assertEquals(403, product1.getStatusCode());

        // 2) 관리자는 같은 요청으로 상품 생성에 성공해야 한다.
        Response admin = e2EClient.postJson(
                "/api/auth/login",
                null,
                TestFixtures.adminLogin()
        );

        assertEquals(200, admin.getStatusCode());
        String adminAccessToken = E2EExtractors.accessToken(admin);
        assertNotNull(adminAccessToken);
        assertFalse(adminAccessToken.isBlank());

        Response product2 = e2EClient.postJson(
                "/api/products",
                adminAccessToken,
                createProductBody
        );
        assertEquals(200, product2.getStatusCode());

        // 3) 생성된 상품은 비로그인 상태에서도 단건 조회가 가능해야 한다.
        Long productId = E2EExtractors.productId(product2);
        Response response = e2EClient.get("/api/products/" + productId, null);
        assertEquals(200, response.getStatusCode());
    }
}
