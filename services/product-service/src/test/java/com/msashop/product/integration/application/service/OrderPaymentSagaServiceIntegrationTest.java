package com.msashop.product.integration.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTopics;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.payload.StockReservationItemPayload;
import com.msashop.common.event.payload.StockReservationRequestedPayload;
import com.msashop.product.adapter.out.persistence.adapter.OutboxEventPersistenceAdapter;
import com.msashop.product.adapter.out.persistence.adapter.ProcessedEventPersistenceAdapter;
import com.msashop.product.adapter.out.persistence.adapter.StockReservationPersistenceAdapter;
import com.msashop.product.adapter.out.persistence.entity.ProductEntity;
import com.msashop.product.adapter.out.persistence.repo.OutboxEventJpaRepository;
import com.msashop.product.adapter.out.persistence.repo.ProductCommandJpaRepository;
import com.msashop.product.adapter.out.persistence.repo.StockReservationJpaRepository;
import com.msashop.product.application.event.ProductSagaEventFactory;
import com.msashop.product.application.service.OrderPaymentSagaService;
import com.msashop.product.application.service.StockReservationLocalTxService;
import com.msashop.product.config.JpaAuditConfig;
import com.msashop.product.domain.model.ProductStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.kafka.consumers.auth-user-saga-enabled=false",
        "app.kafka.producer.relay-enabled=false"
})
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        OrderPaymentSagaService.class,
        StockReservationLocalTxService.class,
        StockReservationPersistenceAdapter.class,
        OutboxEventPersistenceAdapter.class,
        ProcessedEventPersistenceAdapter.class,
        ProductSagaEventFactory.class,
        JpaAuditConfig.class,
        OrderPaymentSagaServiceIntegrationTest.TestConfig.class
})
class OrderPaymentSagaServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("product_service_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private OrderPaymentSagaService service;

    @Autowired
    private ProductCommandJpaRepository productCommandJpaRepository;

    @Autowired
    private StockReservationJpaRepository stockReservationJpaRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    @AfterEach
    void resetDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    stock_reservation_item,
                    stock_reservation,
                    outbox_event,
                    processed_event,
                    product
                RESTART IDENTITY CASCADE
                """);
    }

    @Test
    @DisplayName("재고 부족이면 예약은 롤백되고 실패 outbox와 processed_event만 남는다")
    void should_append_failed_outbox_when_stock_is_short() throws Exception {
        ProductEntity product = productCommandJpaRepository.save(ProductEntity.builder()
                .productName("테스트 상품")
                .price(new BigDecimal("10000"))
                .stock(1)
                .status(ProductStatus.ON_SALE)
                .useYn(true)
                .build());

        StockReservationRequestedPayload payload = new StockReservationRequestedPayload(
                1L,
                1L,
                new BigDecimal("20000"),
                "KRW",
                "idem-1",
                "FAKE",
                List.of(new StockReservationItemPayload(product.getProductId(), 2))
        );

        EventEnvelope envelope = new EventEnvelope(
                "event-1",
                EventTypes.STOCK_RESERVATION_REQUESTED,
                "ORDER",
                "1",
                "saga-1",
                "corr-1",
                "cause-1",
                "order-service",
                EventTopics.ORDER_PAYMENT_SAGA_V1,
                "1",
                Instant.now(),
                objectMapper.writeValueAsString(payload)
        );

        boolean handled = service.handle("product-saga-group", "worker-1", 60, envelope);

        assertThat(handled).isTrue();
        assertThat(productCommandJpaRepository.findById(product.getProductId()).orElseThrow().getStock()).isEqualTo(1);
        assertThat(stockReservationJpaRepository.findAll()).isEmpty();
        assertThat(outboxEventJpaRepository.findAll())
                .singleElement()
                .extracting(event -> event.getEventType())
                .isEqualTo(EventTypes.STOCK_RESERVATION_FAILED);
        assertThat(loadProcessedStatus("product-saga-group", "event-1")).isEqualTo("PROCESSED");
    }

    private String loadProcessedStatus(String consumerGroup, String eventId) {
        return jdbcTemplate.queryForObject(
                "select status from processed_event where consumer_group = ? and event_id = ?",
                String.class,
                consumerGroup,
                eventId
        );
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }
}
