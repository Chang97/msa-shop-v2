package com.msashop.e2e;

import com.msashop.e2e.support.E2EClient;
import com.msashop.e2e.support.E2EExtractors;
import com.msashop.e2e.support.PollingSupport;
import com.msashop.e2e.support.TestFixtures;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentE2ETest {

    private final E2EClient client = new E2EClient();

    @Test
    void pay_order_saga_success_should_eventually_mark_paid_and_decrease_stock() {
        int initialStock = 5;
        BigDecimal unitPrice = BigDecimal.valueOf(2000);

        String adminToken = client.loginAdminToken();
        Long productId = createProduct(adminToken, "E2E Saga Success Product", unitPrice, initialStock);

        String userToken = client.loginUserToken();
        Long orderId = createOrder(userToken, productId, "E2E Saga Success Product", unitPrice, 1);

        Response pay = client.postJson(
                "/api/orders/" + orderId + "/pay",
                userToken,
                TestFixtures.pay("idem-success-" + UUID.randomUUID(), "FAKE")
        );
        assertTrue(is2xx(pay.statusCode()));

        Response paidOrder = PollingSupport.pollUntil(
                Duration.ofSeconds(10),
                Duration.ofMillis(500),
                () -> client.get("/api/orders/" + orderId, userToken),
                response -> response.statusCode() == 200
                        && "PAID".equals(E2EExtractors.orderStatus(response))
        );
        assertEquals("PAID", E2EExtractors.orderStatus(paidOrder));

        Response productDetail = PollingSupport.pollUntil(
                Duration.ofSeconds(10),
                Duration.ofMillis(500),
                () -> client.get("/api/products/" + productId, null),
                response -> response.statusCode() == 200
                        && response.jsonPath().getInt("stock") == initialStock - 1
        );
        assertEquals(initialStock - 1, productDetail.jsonPath().getInt("stock"));
    }

    @Test
    void pay_order_saga_fail_should_keep_pending_and_restore_stock() {
        int initialStock = 5;
        BigDecimal unitPrice = BigDecimal.valueOf(3000);

        String adminToken = client.loginAdminToken();
        Long productId = createProduct(adminToken, "E2E Saga Fail Product", unitPrice, initialStock);

        String userToken = client.loginUserToken();
        Long orderId = createOrder(userToken, productId, "E2E Saga Fail Product", unitPrice, 1);

        Response pay = client.postJson(
                "/api/orders/" + orderId + "/pay",
                userToken,
                TestFixtures.pay("FAIL-" + UUID.randomUUID(), "FAKE")
        );
        assertTrue(is2xx(pay.statusCode()));

        Response orderDetail = PollingSupport.pollUntil(
                Duration.ofSeconds(10),
                Duration.ofMillis(500),
                () -> client.get("/api/orders/" + orderId, userToken),
                response -> response.statusCode() == 200
                        && "PENDING_PAYMENT".equals(E2EExtractors.orderStatus(response))
        );
        assertEquals("PENDING_PAYMENT", E2EExtractors.orderStatus(orderDetail));

        Response productDetail = PollingSupport.pollUntil(
                Duration.ofSeconds(10),
                Duration.ofMillis(500),
                () -> client.get("/api/products/" + productId, null),
                response -> response.statusCode() == 200
                        && response.jsonPath().getInt("stock") == initialStock
        );
        assertEquals(initialStock, productDetail.jsonPath().getInt("stock"));
    }

    @Test
    void paid_order_cannot_cancel() {
        int initialStock = 5;
        BigDecimal unitPrice = BigDecimal.valueOf(4000);

        String adminToken = client.loginAdminToken();
        Long productId = createProduct(adminToken, "E2E Paid Cancel Product", unitPrice, initialStock);

        String userToken = client.loginUserToken();
        Long orderId = createOrder(userToken, productId, "E2E Paid Cancel Product", unitPrice, 1);

        Response pay = client.postJson(
                "/api/orders/" + orderId + "/pay",
                userToken,
                TestFixtures.pay("idem-paid-cancel-" + UUID.randomUUID(), "FAKE")
        );
        assertTrue(is2xx(pay.statusCode()));

        Response paidOrder = PollingSupport.pollUntil(
                Duration.ofSeconds(10),
                Duration.ofMillis(500),
                () -> client.get("/api/orders/" + orderId, userToken),
                response -> response.statusCode() == 200
                        && "PAID".equals(E2EExtractors.orderStatus(response))
        );
        assertEquals("PAID", E2EExtractors.orderStatus(paidOrder));

        Response cancel = client.postJson(
                "/api/orders/" + orderId + "/cancel",
                userToken,
                TestFixtures.cancel("paid order cancel test")
        );
        assertEquals(409, cancel.statusCode());
    }

    private Long createProduct(String adminToken, String productName, BigDecimal unitPrice, int initialStock) {
        Response createProduct = client.postJson(
                "/api/products",
                adminToken,
                TestFixtures.product(productName, unitPrice.intValue(), initialStock)
        );
        assertTrue(is2xx(createProduct.statusCode()));
        return E2EExtractors.productId(createProduct);
    }

    private Long createOrder(String userToken, Long productId, String productName, BigDecimal unitPrice, int quantity) {
        TestFixtures.CreateOrderRequest orderRequest = new TestFixtures.CreateOrderRequest(
                "KRW",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "PaymentTester",
                "010-0000-0000",
                "00000",
                "Seoul Test St 1",
                null,
                null,
                List.of(new TestFixtures.OrderItemRequest(productId, productName, unitPrice, quantity))
        );

        Response createOrder = client.postJson("/api/orders", userToken, orderRequest);
        assertTrue(is2xx(createOrder.statusCode()));
        return E2EExtractors.orderId(createOrder);
    }

    private static boolean is2xx(int status) {
        return status >= 200 && status < 300;
    }
}
