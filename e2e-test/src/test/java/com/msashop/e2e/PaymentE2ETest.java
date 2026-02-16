package com.msashop.e2e;

import com.msashop.e2e.support.E2EClient;
import com.msashop.e2e.support.E2EExtractors;
import com.msashop.e2e.support.TestFixtures;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PaymentE2ETest {

    private final E2EClient client = new E2EClient();

    @Test
    void approve_payment_transitions_to_paid_and_decreases_stock() {
        int initialStock = 5;
        BigDecimal unitPrice = BigDecimal.valueOf(2000);

        // 1) 관리자 로그인
        Response adminLogin = client.postJson("/api/auth/login", null, TestFixtures.adminLogin());
        assertTrue(is2xx(adminLogin.statusCode()));
        String adminToken = E2EExtractors.accessToken(adminLogin);

        // 2) 상품 생성
        Response createProduct = client.postJson(
                "/api/products",
                adminToken,
                TestFixtures.product("E2E Payment Product", unitPrice.intValue(), initialStock)
        );
        assertTrue(createProduct.statusCode() == 200 || createProduct.statusCode() == 201);
        Long productId = E2EExtractors.productId(createProduct);

        // 3) 사용자 로그인
        Response userLogin = client.postJson("/api/auth/login", null, TestFixtures.userLogin());
        assertTrue(is2xx(userLogin.statusCode()));
        String userToken = E2EExtractors.accessToken(userLogin);

        // 4) 주문 생성
        TestFixtures.CreateOrderRequest orderRequest = new TestFixtures.CreateOrderRequest(
                "KRW", BigDecimal.ZERO, BigDecimal.ZERO,
                "PaymentTester", "010-0000-0000", "00000", "Seoul Test St 1",
                null, null,
                List.of(new TestFixtures.OrderItemRequest(productId, "E2E Payment Product", unitPrice, 1))
        );
        Response createOrder = client.postJson("/api/orders", userToken, orderRequest);
        assertTrue(createOrder.statusCode() == 200 || createOrder.statusCode() == 201);
        Long orderId = E2EExtractors.orderId(createOrder);

        // 5) 결제 시작 → PENDING_PAYMENT
        Response pay = client.postJson("/api/orders/" + orderId + "/pay", userToken, null);
        assertTrue(pay.statusCode() == 200 || pay.statusCode() == 204);

        Response pendingDetail = client.get("/api/orders/" + orderId, userToken);
        assertEquals(200, pendingDetail.statusCode());
        assertEquals("PENDING_PAYMENT", E2EExtractors.orderStatus(pendingDetail));

        // 6) 결제 승인 (모의)
        String idempotencyKey = UUID.randomUUID().toString();
        BigDecimal paymentAmount = orderRequest.items().stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .subtract(orderRequest.discountAmount())
                .add(orderRequest.shippingFee());

        Response approve = client.postJson(
                "/api/payments/approve",
                userToken,
                TestFixtures.approvePayment(orderId, paymentAmount, idempotencyKey)
        );
        assertEquals(200, approve.statusCode());
        String paymentStatus = approve.jsonPath().getString("status");
        assertEquals("APPROVED", paymentStatus);

        // 7) 최종 상태 및 재고 확인
        Response orderDetail = client.get("/api/orders/" + orderId, userToken);
        assertEquals(200, orderDetail.statusCode());
        String status = E2EExtractors.orderStatus(orderDetail);
        assertEquals("PAID", status);

        Response productDetail = client.get("/api/products/" + productId, null);
        assertEquals(200, productDetail.statusCode());
        Integer stock = productDetail.jsonPath().getInt("stock");
        assertEquals(initialStock - 1, stock.intValue());
    }

    @Test
    void paid_order_cannot_cancel() {
        int initialStock = 5;
        BigDecimal unitPrice = BigDecimal.valueOf(2000);

        // 1) 관리자 로그인
        Response adminLogin = client.postJson("/api/auth/login", null, TestFixtures.adminLogin());
        assertTrue(is2xx(adminLogin.statusCode()));
        String adminToken = E2EExtractors.accessToken(adminLogin);

        // 2) 상품 생성
        Response createProduct = client.postJson(
                "/api/products",
                adminToken,
                TestFixtures.product("E2E Payment Product", unitPrice.intValue(), initialStock)
        );
        assertTrue(createProduct.statusCode() == 200 || createProduct.statusCode() == 201);
        Long productId = E2EExtractors.productId(createProduct);

        // 3) 사용자 로그인
        Response userLogin = client.postJson("/api/auth/login", null, TestFixtures.userLogin());
        assertTrue(is2xx(userLogin.statusCode()));
        String userToken = E2EExtractors.accessToken(userLogin);

        // 4) 주문 생성
        TestFixtures.CreateOrderRequest orderRequest = new TestFixtures.CreateOrderRequest(
                "KRW", BigDecimal.ZERO, BigDecimal.ZERO,
                "PaymentTester", "010-0000-0000", "00000", "Seoul Test St 1",
                null, null,
                List.of(new TestFixtures.OrderItemRequest(productId, "E2E Payment Product", unitPrice, 1))
        );
        Response createOrder = client.postJson("/api/orders", userToken, orderRequest);
        assertTrue(createOrder.statusCode() == 200 || createOrder.statusCode() == 201);
        Long orderId = E2EExtractors.orderId(createOrder);

        // 5) 결제 시작 → PENDING_PAYMENT
        Response pay = client.postJson("/api/orders/" + orderId + "/pay", userToken, null);
        assertTrue(pay.statusCode() == 200 || pay.statusCode() == 204);

        Response pendingDetail = client.get("/api/orders/" + orderId, userToken);
        assertEquals(200, pendingDetail.statusCode());
        assertEquals("PENDING_PAYMENT", E2EExtractors.orderStatus(pendingDetail));

        // 6) 결제 승인 (모의)
        String idempotencyKey = UUID.randomUUID().toString();
        BigDecimal paymentAmount = orderRequest.items().stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .subtract(orderRequest.discountAmount())
                .add(orderRequest.shippingFee());

        Response approve = client.postJson(
                "/api/payments/approve",
                userToken,
                TestFixtures.approvePayment(orderId, paymentAmount, idempotencyKey)
        );
        assertEquals(200, approve.statusCode());
        assertEquals("APPROVED", approve.jsonPath().getString("status"));

        // 7) 결제 완료된 주문 취소 시도 → 실패(409/400)
        Response cancel = client.postJson(
                "/api/orders/" + orderId + "/cancel",
                userToken,
                TestFixtures.cancel("PAID 상태 취소 테스트")
        );
        assertEquals(409, cancel.statusCode());
    }

    @Test
    public void approve_payment_idempotent_same_key_no_double_stock() {
        int initialStock = 5;
        BigDecimal unitPrice = BigDecimal.valueOf(2000);

        // 1) 관리자 로그인
        Response adminLogin = client.postJson("/api/auth/login", null, TestFixtures.adminLogin());
        assertTrue(is2xx(adminLogin.statusCode()));
        String adminToken = E2EExtractors.accessToken(adminLogin);

        // 2) 상품 생성
        Response createProduct = client.postJson(
                "/api/products",
                adminToken,
                TestFixtures.product("E2E Payment Product", unitPrice.intValue(), initialStock)
        );
        assertTrue(createProduct.statusCode() == 200 || createProduct.statusCode() == 201);
        Long productId = E2EExtractors.productId(createProduct);

        // 3) 사용자 로그인
        Response userLogin = client.postJson("/api/auth/login", null, TestFixtures.userLogin());
        assertTrue(is2xx(userLogin.statusCode()));
        String userToken = E2EExtractors.accessToken(userLogin);

        // 4) 주문 생성
        TestFixtures.CreateOrderRequest orderRequest = new TestFixtures.CreateOrderRequest(
                "KRW", BigDecimal.ZERO, BigDecimal.ZERO,
                "PaymentTester", "010-0000-0000", "00000", "Seoul Test St 1",
                null, null,
                List.of(new TestFixtures.OrderItemRequest(productId, "E2E Payment Product", unitPrice, 1))
        );
        Response createOrder = client.postJson("/api/orders", userToken, orderRequest);
        assertTrue(createOrder.statusCode() == 200 || createOrder.statusCode() == 201);
        Long orderId = E2EExtractors.orderId(createOrder);

        // 5) 결제 시작 → PENDING_PAYMENT
        // 같은 idempotencyKey로 approve 2번
        Response pay = client.postJson("/api/orders/" + orderId + "/pay", userToken, null);
        assertTrue(pay.statusCode() == 200 || pay.statusCode() == 204);

        Response pendingDetail = client.get("/api/orders/" + orderId, userToken);
        assertEquals(200, pendingDetail.statusCode());
        assertEquals("PENDING_PAYMENT", E2EExtractors.orderStatus(pendingDetail));

        // 6) 결제 승인 (모의)
        String idempotencyKey = UUID.randomUUID().toString();
        BigDecimal paymentAmount = orderRequest.items().stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .subtract(orderRequest.discountAmount())
                .add(orderRequest.shippingFee());

        Response approve1 = client.postJson(
                "/api/payments/approve",
                userToken,
                TestFixtures.approvePayment(orderId, paymentAmount, idempotencyKey)
        );

        Response approve2 = client.postJson(
                "/api/payments/approve",
                userToken,
                TestFixtures.approvePayment(orderId, paymentAmount, idempotencyKey)
        );
        assertEquals(200, approve2.statusCode());
        String paymentStatus = approve2.jsonPath().getString("status");
        assertEquals("APPROVED", paymentStatus);

        // 7) 최종 상태 및 재고 확인
        Response orderDetail = client.get("/api/orders/" + orderId, userToken);
        assertEquals(200, orderDetail.statusCode());
        String status = E2EExtractors.orderStatus(orderDetail);
        assertEquals("PAID", status);

        Response productDetail = client.get("/api/products/" + productId, null);
        assertEquals(200, productDetail.statusCode());
        Integer stock = productDetail.jsonPath().getInt("stock");
        assertEquals(initialStock - 1, stock.intValue());
    }

    private static boolean is2xx(int status) {
        return status >= 200 && status < 300;
    }
}
