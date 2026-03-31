package com.msashop.order.integration.application.service;

import com.msashop.order.adapter.out.persistence.adapter.OrderPersistenceAdapter;
import com.msashop.order.adapter.out.persistence.entity.OrderStatusHistoryEntity;
import com.msashop.order.adapter.out.persistence.repo.OrderStatusHistoryJpaRepository;
import com.msashop.order.application.service.PendingPaymentExpirationService;
import com.msashop.order.config.JpaAuditConfig;
import com.msashop.order.domain.model.Order;
import com.msashop.order.domain.model.OrderItem;
import com.msashop.order.domain.model.OrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        OrderPersistenceAdapter.class,
        PendingPaymentExpirationService.class,
        JpaAuditConfig.class
})
class PendingPaymentExpirationServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("order_service_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private OrderPersistenceAdapter orderPersistenceAdapter;

    @Autowired
    private PendingPaymentExpirationService service;

    @Autowired
    private OrderStatusHistoryJpaRepository orderStatusHistoryJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    @DisplayName("결제 진행 중 주문은 만료 시간이 지나면 PAYMENT_EXPIRED와 이력이 저장된다")
    void should_expire_pending_payment_and_save_history() {
        Order order = Order.create(
                "ORD-IT-001",
                1L,
                "KRW",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "홍길동",
                "010-1111-2222",
                "12345",
                "서울시 강남구",
                "101동 1001호",
                null,
                List.of(new OrderItem(10L, "테스트 상품", new BigDecimal("10000"), 1))
        );
        order.startPayment();

        Long orderId = orderPersistenceAdapter.save(order);
        Instant threshold = Instant.now().minusSeconds(900);
        jdbcTemplate.update(
                "update orders set updated_at = ? where id = ?",
                Timestamp.from(threshold.minusSeconds(60)),
                orderId
        );

        service.expirePendingPayments(threshold, 10);

        assertThat(orderPersistenceAdapter.loadOrder(orderId).getStatus()).isEqualTo(OrderStatus.PAYMENT_EXPIRED);
        assertThat(orderStatusHistoryJpaRepository.findAll())
                .singleElement()
                .extracting(OrderStatusHistoryEntity::getToStatus, OrderStatusHistoryEntity::getReason)
                .containsExactly(OrderStatus.PAYMENT_EXPIRED, "PAYMENT_EXPIRED");
    }
}
