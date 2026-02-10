package com.msashop.payment.integration;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true"
})
class PaymentE2EIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.2-alpine")
            .withDatabaseName("payment_db")
            .withUsername("payment_user")
            .withPassword("payment_pw");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("clients.order.base-url", () -> System.getProperty("test.order-base-url", "http://localhost:8083"));
        registry.add("clients.product.base-url", () -> System.getProperty("test.product-base-url", "http://localhost:8082"));
    }

    @LocalServerPort
    int paymentPort;

    RestTemplate restTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Value("${test.product-base-url:http://localhost:8082}")
    String productBaseUrl;

    @Value("${test.order-base-url:http://localhost:8083}")
    String orderBaseUrl;

    private static final HttpHeaders USER_HEADERS;
    private static final HttpHeaders ADMIN_HEADERS;

    static {
        USER_HEADERS = new HttpHeaders();
        USER_HEADERS.set("X-User-Id", "1");
        USER_HEADERS.set("X-Roles", "ROLE_USER");

        ADMIN_HEADERS = new HttpHeaders();
        ADMIN_HEADERS.set("X-User-Id", "1");
        ADMIN_HEADERS.set("X-Roles", "ROLE_ADMIN,ROLE_USER");
    }

    @Autowired
    void initRestTemplate(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    @Test
    void endToEnd_payment_approves_and_is_idempotent() {
        assumeServiceHealthy(productBaseUrl + "/actuator/health");
        assumeServiceHealthy(orderBaseUrl + "/actuator/health");

        // 1) product 준비
        BigDecimal price = BigDecimal.valueOf(1000);
        int initialStock = 10;
        String productName = "TestProduct-" + UUID.randomUUID();
        Long productId = createProduct(productName, price, initialStock);
        ProductResponse productBefore = getProduct(productId);

        // 2) 주문 생성
        int quantity = 2;
        Long orderId = createOrder(productId, productName, price, quantity);

        // 3) 결제 시작
        startPayment(orderId);

        // 4) 결제 승인 (idempotent key)
        String idempotencyKey = "PAY-" + orderId + "-TEST";
        PaymentApproveResponse firstApprove = approvePayment(orderId, price.multiply(BigDecimal.valueOf(quantity)), idempotencyKey);

        // 5) 상태/재고 검증
        OrderResponse orderAfterFirst = getOrder(orderId);
        ProductResponse productAfterFirst = getProduct(productId);
        int paymentCountAfterFirst = countPayments(idempotencyKey);

        assertThat(firstApprove.paymentId()).isNotNull();
        assertThat(firstApprove.status()).isEqualTo("APPROVED");
        assertThat(orderAfterFirst.status()).isEqualTo("PAID");
        assertThat(productAfterFirst.stock()).isEqualTo(productBefore.stock() - quantity);
        assertThat(paymentCountAfterFirst).isEqualTo(1);

        // 6) 같은 키로 재승인
        PaymentApproveResponse secondApprove = approvePayment(orderId, price.multiply(BigDecimal.valueOf(quantity)), idempotencyKey);
        OrderResponse orderAfterSecond = getOrder(orderId);
        ProductResponse productAfterSecond = getProduct(productId);
        int paymentCountAfterSecond = countPayments(idempotencyKey);

        assertThat(secondApprove.paymentId()).isEqualTo(firstApprove.paymentId());
        assertThat(productAfterSecond.stock()).isEqualTo(productAfterFirst.stock());
        assertThat(paymentCountAfterSecond).isEqualTo(1);
        assertThat(orderAfterSecond.status()).isEqualTo("PAID");
    }

    private void assumeServiceHealthy(String url) {
        try {
            ResponseEntity<String> res = restTemplate.getForEntity(url, String.class);
            Assumptions.assumeTrue(res.getStatusCode().is2xxSuccessful(), "Service not healthy: " + url);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Service not reachable: " + url + " - " + e.getMessage());
        }
    }

    private Long createProduct(String name, BigDecimal price, int stock) {
        Map<String, Object> body = Map.of(
                "productName", name,
                "price", price,
                "stock", stock
        );
        ResponseEntity<Long> res = exchange(productBaseUrl + "/api/products", HttpMethod.POST, body, Long.class, ADMIN_HEADERS);
        return res.getBody();
    }

    private ProductResponse getProduct(Long productId) {
        ResponseEntity<ProductResponse> res = exchange(productBaseUrl + "/api/products/" + productId, HttpMethod.GET, null, ProductResponse.class, USER_HEADERS);
        return res.getBody();
    }

    private Long createOrder(Long productId, String productName, BigDecimal price, int quantity) {
        Map<String, Object> item = Map.of(
                "productId", productId,
                "productName", productName,
                "unitPrice", price,
                "quantity", quantity
        );
        Map<String, Object> payload = Map.of(
                "currency", "KRW",
                "discountAmount", BigDecimal.ZERO,
                "shippingFee", BigDecimal.ZERO,
                "receiverName", "수취인",
                "receiverPhone", "010-0000-0000",
                "shippingPostcode", "12345",
                "shippingAddress1", "주소1",
                "shippingAddress2", "주소2",
                "memo", "테스트",
                "items", List.of(item)
        );
        ResponseEntity<Long> res = exchange(orderBaseUrl + "/api/orders", HttpMethod.POST, payload, Long.class, USER_HEADERS);
        return res.getBody();
    }

    private void startPayment(Long orderId) {
        exchange(orderBaseUrl + "/api/orders/" + orderId + "/pay", HttpMethod.POST, null, Void.class, USER_HEADERS);
    }

    private PaymentApproveResponse approvePayment(Long orderId, BigDecimal amount, String key) {
        Map<String, Object> payload = Map.of(
                "orderId", orderId,
                "amount", amount,
                "idempotencyKey", key
        );
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(USER_HEADERS);
        String url = "http://localhost:" + paymentPort + "/api/payments/approve";
        ResponseEntity<PaymentApproveResponse> res = exchange(url, HttpMethod.POST, payload, PaymentApproveResponse.class, headers);
        return res.getBody();
    }

    private OrderResponse getOrder(Long orderId) {
        ResponseEntity<OrderResponse> res = exchange(orderBaseUrl + "/api/orders/" + orderId, HttpMethod.GET, null, OrderResponse.class, USER_HEADERS);
        return res.getBody();
    }

    private int countPayments(String idempotencyKey) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from payment_transaction where idempotency_key = ?", Integer.class, idempotencyKey);
        return count == null ? 0 : count;
    }

    private <T> ResponseEntity<T> exchange(String url, HttpMethod method, Object body, Class<T> type, HttpHeaders headers) {
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(url, method, entity, type);
    }

    private record ProductResponse(Long productId, String productName, BigDecimal price, Integer stock, String status, Boolean useYn) {}

    private record OrderResponse(Long orderId, String status) {}

    private record PaymentApproveResponse(Long paymentId, Long orderId, String status) {}
}
