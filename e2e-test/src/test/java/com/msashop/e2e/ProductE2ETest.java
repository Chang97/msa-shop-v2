package com.msashop.e2e;


import com.msashop.e2e.support.E2EClient;
import com.msashop.e2e.support.E2EExtractors;
import com.msashop.e2e.support.TestFixtures;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

public class ProductE2ETest {
    private final E2EClient e2EClient = new E2EClient();

    @Test
    void public_can_get_product_list() {
        Response listResp = e2EClient.get("/api/products", null);
        assertEquals(200, listResp.getStatusCode());

        var items = listResp.jsonPath().getList("$");
        assertNotNull(items);
    }

    @Test
    public void only_admin_can_create_product() {
        // 1) 일반 사용자 상품 추가 실패 테스트
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

        // 2) 관리자 상품 추가 성공 테스트
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

        // 3) public으로 product 단건 조회 테스트
        Long productId = E2EExtractors.productId(product2);
        Response response = e2EClient.get("/api/products/" + productId, null);
        assertEquals(200, response.getStatusCode());
    }
}
