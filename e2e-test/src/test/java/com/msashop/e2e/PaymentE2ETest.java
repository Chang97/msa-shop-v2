package com.msashop.e2e;

import com.msashop.e2e.support.E2EClient;
import com.msashop.e2e.support.E2EDbSupport;
import com.msashop.e2e.support.E2EExtractors;
import com.msashop.e2e.support.PollingSupport;
import com.msashop.e2e.support.TestFixtures;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 결제 Saga 의 성공, 실패, 불명확 결과, 만료, 취소 제한 흐름을 검증한다.
class PaymentE2ETest {

    private final E2EClient client = new E2EClient();
    private final E2EDbSupport dbSupport = new E2EDbSupport();

    // 결제 성공 후 주문과 재고 상태가 최종적으로 일관되게 반영되는지 검증한다.
    @Test
    void pay_order_saga_success_should_eventually_mark_paid_and_decrease_stock() {
        // 결제 승인 시 주문은 PAID, 예약 재고는 최종 차감 상태로 수렴해야 한다.
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

    // PG 실패 시 주문 실패와 재고 복구가 함께 반영되는지 검증한다.
    @Test
    void pay_order_pg_fail_should_mark_payment_failed_and_restore_stock() {
        // PG 실패 시 주문은 PAYMENT_FAILED 로 끝나고 예약한 재고는 원복되어야 한다.
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
                        && "PAYMENT_FAILED".equals(E2EExtractors.orderStatus(response))
        );
        assertEquals("PAYMENT_FAILED", E2EExtractors.orderStatus(orderDetail));

        Response productDetail = PollingSupport.pollUntil(
                Duration.ofSeconds(10),
                Duration.ofMillis(500),
                () -> client.get("/api/products/" + productId, null),
                response -> response.statusCode() == 200
                        && response.jsonPath().getInt("stock") == initialStock
        );
        assertEquals(initialStock, productDetail.jsonPath().getInt("stock"));
    }

    // 재고 부족이 결제 단계에서 실패로 처리되고 재고가 유지되는지 검증한다.
    @Test
    void pay_order_stock_shortage_should_mark_payment_failed_without_decreasing_stock() {
        // 재고 부족은 주문 생성이 아니라 결제 단계의 재고 예약에서 실패해야 한다.
        int initialStock = 1;
        BigDecimal unitPrice = BigDecimal.valueOf(3500);

        String adminToken = client.loginAdminToken();
        Long productId = createProduct(adminToken, "E2E Stock Shortage Product", unitPrice, initialStock);

        String userToken = client.loginUserToken();
        Long orderId = createOrder(userToken, productId, "E2E Stock Shortage Product", unitPrice, 2);

        Response pay = client.postJson(
                "/api/orders/" + orderId + "/pay",
                userToken,
                TestFixtures.pay("idem-stock-shortage-" + UUID.randomUUID(), "FAKE")
        );
        assertTrue(is2xx(pay.statusCode()));

        Response orderDetail = PollingSupport.pollUntil(
                Duration.ofSeconds(10),
                Duration.ofMillis(500),
                () -> client.get("/api/orders/" + orderId, userToken),
                response -> response.statusCode() == 200
                        && "PAYMENT_FAILED".equals(E2EExtractors.orderStatus(response))
        );
        assertEquals("PAYMENT_FAILED", E2EExtractors.orderStatus(orderDetail));

        Response productDetail = PollingSupport.pollUntil(
                Duration.ofSeconds(10),
                Duration.ofMillis(500),
                () -> client.get("/api/products/" + productId, null),
                response -> response.statusCode() == 200
                        && response.jsonPath().getInt("stock") == initialStock
        );
        assertEquals(initialStock, productDetail.jsonPath().getInt("stock"));
    }

    // PG 결과가 불명확하면 APPROVAL_UNKNOWN 으로 저장된 뒤 reconciliation 으로 승인 확정되는지 검증한다.
    @Test
    void approval_unknown_should_eventually_reconcile_to_paid() {
        int initialStock = 5;
        BigDecimal unitPrice = BigDecimal.valueOf(2700);
        String idempotencyKey = "UNKNOWN-APPROVE-" + UUID.randomUUID();

        String adminToken = client.loginAdminToken();
        Long productId = createProduct(adminToken, "E2E Unknown Approve Product", unitPrice, initialStock);

        String userToken = client.loginUserToken();
        Long orderId = createOrder(userToken, productId, "E2E Unknown Approve Product", unitPrice, 1);

        Response pay = client.postJson(
                "/api/orders/" + orderId + "/pay",
                userToken,
                TestFixtures.pay(idempotencyKey, "FAKE")
        );
        assertTrue(is2xx(pay.statusCode()));

        String unknownStatus = PollingSupport.pollUntil(
                Duration.ofSeconds(10),
                Duration.ofMillis(500),
                () -> dbSupport.findPaymentStatusByIdempotencyKey(idempotencyKey),
                "APPROVAL_UNKNOWN"::equals
        );
        assertEquals("APPROVAL_UNKNOWN", unknownStatus);

        String approvedStatus = PollingSupport.pollUntil(
                Duration.ofSeconds(15),
                Duration.ofMillis(500),
                () -> dbSupport.findPaymentStatusByIdempotencyKey(idempotencyKey),
                "APPROVED"::equals
        );
        assertEquals("APPROVED", approvedStatus);

        Response paidOrder = PollingSupport.pollUntil(
                Duration.ofSeconds(15),
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

    // PG 결과가 불명확하고 reconciliation 결과도 실패면 주문 실패와 재고 복구로 수렴하는지 검증한다.
    @Test
    void approval_unknown_fail_should_eventually_mark_payment_failed_and_restore_stock() {
        int initialStock = 5;
        BigDecimal unitPrice = BigDecimal.valueOf(2800);
        String idempotencyKey = "UNKNOWN-FAIL-" + UUID.randomUUID();

        String adminToken = client.loginAdminToken();
        Long productId = createProduct(adminToken, "E2E Unknown Fail Product", unitPrice, initialStock);

        String userToken = client.loginUserToken();
        Long orderId = createOrder(userToken, productId, "E2E Unknown Fail Product", unitPrice, 1);

        Response pay = client.postJson(
                "/api/orders/" + orderId + "/pay",
                userToken,
                TestFixtures.pay(idempotencyKey, "FAKE")
        );
        assertTrue(is2xx(pay.statusCode()));

        String unknownStatus = PollingSupport.pollUntil(
                Duration.ofSeconds(10),
                Duration.ofMillis(500),
                () -> dbSupport.findPaymentStatusByIdempotencyKey(idempotencyKey),
                "APPROVAL_UNKNOWN"::equals
        );
        assertEquals("APPROVAL_UNKNOWN", unknownStatus);

        String failedStatus = PollingSupport.pollUntil(
                Duration.ofSeconds(15),
                Duration.ofMillis(500),
                () -> dbSupport.findPaymentStatusByIdempotencyKey(idempotencyKey),
                "FAILED"::equals
        );
        assertEquals("FAILED", failedStatus);

        Response failedOrder = PollingSupport.pollUntil(
                Duration.ofSeconds(15),
                Duration.ofMillis(500),
                () -> client.get("/api/orders/" + orderId, userToken),
                response -> response.statusCode() == 200
                        && "PAYMENT_FAILED".equals(E2EExtractors.orderStatus(response))
        );
        assertEquals("PAYMENT_FAILED", E2EExtractors.orderStatus(failedOrder));

        Response productDetail = PollingSupport.pollUntil(
                Duration.ofSeconds(10),
                Duration.ofMillis(500),
                () -> client.get("/api/products/" + productId, null),
                response -> response.statusCode() == 200
                        && response.jsonPath().getInt("stock") == initialStock
        );
        assertEquals(initialStock, productDetail.jsonPath().getInt("stock"));
    }

    // 결제 진행 중 주문이 timeout 기준을 넘기면 PAYMENT_EXPIRED 로 만료되는지 검증한다.
    @Test
    void pending_payment_should_eventually_expire() {
        BigDecimal unitPrice = BigDecimal.valueOf(2500);

        String adminToken = client.loginAdminToken();
        Long productId = createProduct(adminToken, "E2E Pending Expire Product", unitPrice, 5);

        String userToken = client.loginUserToken();
        Long orderId = createOrder(userToken, productId, "E2E Pending Expire Product", unitPrice, 1);

        dbSupport.updateOrderStatusAndUpdatedAt(
                orderId,
                "PENDING_PAYMENT",
                Instant.now().minusSeconds(1800)
        );

        Response expiredOrder = PollingSupport.pollUntil(
                Duration.ofSeconds(15),
                Duration.ofMillis(500),
                () -> client.get("/api/orders/" + orderId, userToken),
                response -> response.statusCode() == 200
                        && "PAYMENT_EXPIRED".equals(E2EExtractors.orderStatus(response))
        );
        assertEquals("PAYMENT_EXPIRED", E2EExtractors.orderStatus(expiredOrder));
    }

    // 만료된 주문은 재결제를 허용하고, 재시도 후 최종적으로 PAID 로 수렴하는지 검증한다.
    @Test
    void payment_expired_order_can_retry_and_eventually_mark_paid() {
        int initialStock = 5;
        BigDecimal unitPrice = BigDecimal.valueOf(2600);

        String adminToken = client.loginAdminToken();
        Long productId = createProduct(adminToken, "E2E Expired Retry Product", unitPrice, initialStock);

        String userToken = client.loginUserToken();
        Long orderId = createOrder(userToken, productId, "E2E Expired Retry Product", unitPrice, 1);

        dbSupport.updateOrderStatusAndUpdatedAt(
                orderId,
                "PAYMENT_EXPIRED",
                Instant.now().minusSeconds(1800)
        );

        Response pay = client.postJson(
                "/api/orders/" + orderId + "/pay",
                userToken,
                TestFixtures.pay("idem-expired-retry-" + UUID.randomUUID(), "FAKE")
        );
        assertTrue(is2xx(pay.statusCode()));

        Response paidOrder = PollingSupport.pollUntil(
                Duration.ofSeconds(15),
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

    // 결제 완료된 주문은 취소할 수 없는지 검증한다.
    @Test
    void paid_order_cannot_cancel() {
        // 이미 결제가 완료된 주문은 취소 API 에서 거절되어야 한다.
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
