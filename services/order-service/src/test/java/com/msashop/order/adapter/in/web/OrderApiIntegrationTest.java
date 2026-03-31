package com.msashop.order.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.order.adapter.in.web.dto.CancelOrderRequest;
import com.msashop.order.adapter.in.web.dto.CreateOrderRequest;
import com.msashop.order.adapter.in.web.dto.PayOrderRequest;
import com.msashop.order.adapter.out.persistence.entity.OrderEntity;
import com.msashop.order.adapter.out.persistence.entity.OrderItemEntity;
import com.msashop.order.adapter.out.persistence.repo.OrderCommandJpaRepository;
import com.msashop.order.adapter.out.persistence.repo.OrderStatusHistoryJpaRepository;
import com.msashop.order.adapter.out.persistence.repo.OutboxEventJpaRepository;
import com.msashop.order.application.port.out.LoadProductPort;
import com.msashop.order.application.port.out.model.ProductRow;
import com.msashop.order.domain.model.OrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.kafka.consumers.order-payment-saga-enabled=false",
        "app.kafka.producer.relay-enabled=false"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class OrderApiIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("order_service_api_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderCommandJpaRepository orderCommandJpaRepository;

    @Autowired
    private OrderStatusHistoryJpaRepository orderStatusHistoryJpaRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @MockBean
    private LoadProductPort loadProductPort;

    @BeforeEach
    @AfterEach
    void resetDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    order_status_history,
                    outbox_event,
                    processed_event,
                    order_item,
                    orders
                RESTART IDENTITY CASCADE
                """);
    }

    @Test
    @DisplayName("create order API should persist CREATED order and history")
    void should_create_order_via_api() throws Exception {
        when(loadProductPort.loadProducts(anyList())).thenReturn(List.of(
                new ProductRow(10L, "Test Product", new BigDecimal("10000"), 100, "ON_SALE", true)
        ));

        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", 1L)
                        .header("X-Roles", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(
                                "KRW",
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                "Hong",
                                "010-1111-2222",
                                "12345",
                                "Seoul",
                                "101-1001",
                                null,
                                List.of(new CreateOrderRequest.OrderItemRequest(10L, "Test Product", new BigDecimal("10000"), 1))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").isNumber());

        assertThat(orderCommandJpaRepository.findAll()).hasSize(1);
        assertThat(orderStatusHistoryJpaRepository.findAll())
                .hasSize(1)
                .first()
                .satisfies(history -> {
                    assertThat(history.getToStatus()).isEqualTo(OrderStatus.CREATED);
                    assertThat(history.getReason()).isEqualTo("ORDER_CREATED");
                });
    }

    @Test
    @DisplayName("my orders API should return only current user orders")
    void should_return_my_orders_only() throws Exception {
        saveOrder(1L, OrderStatus.CREATED, "ORD-001");
        saveOrder(1L, OrderStatus.PAYMENT_FAILED, "ORD-002");
        saveOrder(2L, OrderStatus.CREATED, "ORD-003");

        mockMvc.perform(get("/api/orders")
                        .header("X-User-Id", 1L)
                        .header("X-Roles", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].orderNumber").isString())
                .andExpect(jsonPath("$[1].orderNumber").isString());
    }

    @Test
    @DisplayName("order detail API should return current user order")
    void should_return_order_detail_for_owner() throws Exception {
        Long orderId = saveOrder(1L, OrderStatus.CREATED, "ORD-001");

        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                        .header("X-User-Id", 1L)
                        .header("X-Roles", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.orderNumber").value("ORD-001"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @DisplayName("order detail API should forbid non-owner access")
    void should_forbid_order_detail_for_non_owner() throws Exception {
        Long orderId = saveOrder(1L, OrderStatus.CREATED, "ORD-001");

        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                        .header("X-User-Id", 2L)
                        .header("X-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("pay order API should change status and append outbox")
    void should_pay_order_via_api() throws Exception {
        Long orderId = saveOrder(1L, OrderStatus.CREATED, "ORD-001");

        mockMvc.perform(post("/api/orders/{orderId}/pay", orderId)
                        .header("X-User-Id", 1L)
                        .header("X-Roles", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PayOrderRequest("idem-001", "FAKE_PG"))))
                .andExpect(status().isNoContent());

        assertThat(orderCommandJpaRepository.findById(orderId))
                .get()
                .extracting(OrderEntity::getStatus)
                .isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(orderStatusHistoryJpaRepository.findAll()).hasSize(1);
        assertThat(outboxEventJpaRepository.findAll())
                .hasSize(1)
                .first()
                .satisfies(outbox -> assertThat(outbox.getEventType()).isEqualTo("StockReservationRequested"));
    }

    @Test
    @DisplayName("cancel order API should cancel failed order")
    void should_cancel_order_via_api() throws Exception {
        Long orderId = saveOrder(1L, OrderStatus.PAYMENT_FAILED, "ORD-001");

        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId)
                        .header("X-User-Id", 1L)
                        .header("X-Roles", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelOrderRequest("user cancel"))))
                .andExpect(status().isNoContent());

        assertThat(orderCommandJpaRepository.findById(orderId))
                .get()
                .extracting(OrderEntity::getStatus)
                .isEqualTo(OrderStatus.CANCELLED);
        assertThat(orderStatusHistoryJpaRepository.findAll())
                .hasSize(1)
                .first()
                .satisfies(history -> assertThat(history.getToStatus()).isEqualTo(OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("cancel order API should reject pending payment order")
    void should_reject_cancel_for_pending_payment_order() throws Exception {
        Long orderId = saveOrder(1L, OrderStatus.PENDING_PAYMENT, "ORD-001");

        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId)
                        .header("X-User-Id", 1L)
                        .header("X-Roles", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelOrderRequest("user cancel"))))
                .andExpect(status().isConflict());

        assertThat(orderCommandJpaRepository.findById(orderId))
                .get()
                .extracting(OrderEntity::getStatus)
                .isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(orderStatusHistoryJpaRepository.findAll()).isEmpty();
    }

    private Long saveOrder(Long userId, OrderStatus status, String orderNumber) {
        OrderEntity order = OrderEntity.builder()
                .orderNumber(orderNumber)
                .userId(userId)
                .status(status)
                .currency("KRW")
                .subtotalAmount(new BigDecimal("10000"))
                .discountAmount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("10000"))
                .receiverName("Hong")
                .receiverPhone("010-1111-2222")
                .shippingPostcode("12345")
                .shippingAddress1("Seoul")
                .shippingAddress2("101-1001")
                .memo(null)
                .build();
        order.setItems(List.of(OrderItemEntity.builder()
                .productId(10L)
                .productName("Test Product")
                .unitPrice(new BigDecimal("10000"))
                .quantity(1)
                .lineAmount(new BigDecimal("10000"))
                .build()));
        return orderCommandJpaRepository.save(order).getId();
    }
}
