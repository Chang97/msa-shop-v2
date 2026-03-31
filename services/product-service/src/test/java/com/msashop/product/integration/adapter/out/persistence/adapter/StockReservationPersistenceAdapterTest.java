package com.msashop.product.integration.adapter.out.persistence.adapter;

import com.msashop.common.event.payload.StockReservationItemPayload;
import com.msashop.product.adapter.out.persistence.adapter.StockReservationPersistenceAdapter;
import com.msashop.product.adapter.out.persistence.entity.ProductEntity;
import com.msashop.product.adapter.out.persistence.entity.StockReservationEntity;
import com.msashop.product.adapter.out.persistence.entity.StockReservationItemEntity;
import com.msashop.product.adapter.out.persistence.repo.ProductCommandJpaRepository;
import com.msashop.product.adapter.out.persistence.repo.StockReservationJpaRepository;
import com.msashop.product.config.JpaAuditConfig;
import com.msashop.product.domain.model.ProductStatus;
import com.msashop.product.domain.model.StockReservationStatus;
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
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        StockReservationPersistenceAdapter.class,
        JpaAuditConfig.class
})
class StockReservationPersistenceAdapterTest {

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
    private StockReservationPersistenceAdapter adapter;

    @Autowired
    private ProductCommandJpaRepository productCommandJpaRepository;

    @Autowired
    private StockReservationJpaRepository stockReservationJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    /**
     * reserve 호출 시 재고 차감과 주문 단위 예약 헤더/아이템 저장이 함께 일어나는지 검증한다.
     */
    @Test
    @DisplayName("reserve 호출 시 재고를 차감하고 예약 헤더와 아이템을 저장한다")
    void should_decrease_stock_and_create_reservation_header_and_items_when_reserve_is_called() {
        ProductEntity product = productCommandJpaRepository.saveAndFlush(ProductEntity.builder()
                .productName("테스트 상품")
                .price(new BigDecimal("10000"))
                .stock(10)
                .status(ProductStatus.ON_SALE)
                .useYn(true)
                .build());

        adapter.reserve(
                "reservation-1",
                1L,
                List.of(new StockReservationItemPayload(product.getProductId(), 3))
        );

        ProductEntity reloadedProduct = productCommandJpaRepository.findById(product.getProductId()).orElseThrow();
        StockReservationEntity reservation = stockReservationJpaRepository.findByReservationId("reservation-1").orElseThrow();

        assertThat(reloadedProduct.getStock()).isEqualTo(7);
        assertThat(reservation.getOrderId()).isEqualTo(1L);
        assertThat(reservation.getStatus()).isEqualTo(StockReservationStatus.RESERVED);
        assertThat(reservation.getItems()).hasSize(1);
        StockReservationItemEntity item = reservation.getItems().get(0);
        assertThat(item.getProductId()).isEqualTo(product.getProductId());
        assertThat(item.getQuantity()).isEqualTo(3);
    }

    /**
     * release 호출 시 RESERVED 예약만 해제하고 차감했던 재고를 복구하는지 검증한다.
     */
    @Test
    @DisplayName("release 호출 시 재고를 복구하고 예약 상태를 RELEASED로 바꾼다")
    void should_restore_stock_and_mark_reservation_released_when_release_is_called() {
        ProductEntity product = productCommandJpaRepository.saveAndFlush(ProductEntity.builder()
                .productName("테스트 상품")
                .price(new BigDecimal("10000"))
                .stock(10)
                .status(ProductStatus.ON_SALE)
                .useYn(true)
                .build());

        adapter.reserve(
                "reservation-2",
                2L,
                List.of(new StockReservationItemPayload(product.getProductId(), 4))
        );

        adapter.release("reservation-2");

        ProductEntity reloadedProduct = productCommandJpaRepository.findById(product.getProductId()).orElseThrow();
        StockReservationEntity reservation = stockReservationJpaRepository.findByReservationId("reservation-2").orElseThrow();

        assertThat(reloadedProduct.getStock()).isEqualTo(10);
        assertThat(reservation.getStatus()).isEqualTo(StockReservationStatus.RELEASED);
    }

    /**
     * confirm 호출 시 재고는 유지하고 예약 상태만 CONFIRMED로 바꾸는지 검증한다.
     */
    @Test
    @DisplayName("confirm 호출 시 예약 상태를 CONFIRMED로 바꾼다")
    void should_mark_reservation_confirmed_when_confirm_is_called() {
        ProductEntity product = productCommandJpaRepository.saveAndFlush(ProductEntity.builder()
                .productName("테스트 상품")
                .price(new BigDecimal("10000"))
                .stock(10)
                .status(ProductStatus.ON_SALE)
                .useYn(true)
                .build());

        adapter.reserve(
                "reservation-3",
                3L,
                List.of(new StockReservationItemPayload(product.getProductId(), 2))
        );

        adapter.confirm("reservation-3");

        ProductEntity reloadedProduct = productCommandJpaRepository.findById(product.getProductId()).orElseThrow();
        StockReservationEntity reservation = stockReservationJpaRepository.findByReservationId("reservation-3").orElseThrow();

        assertThat(reloadedProduct.getStock()).isEqualTo(8);
        assertThat(reservation.getStatus()).isEqualTo(StockReservationStatus.CONFIRMED);
    }

    /**
     * 만료 시각이 지난 RESERVED 예약은 EXPIRED로 바꾸고 재고를 복구하는지 검증한다.
     */
    @Test
    @DisplayName("만료된 RESERVED 예약은 EXPIRED로 바꾸고 재고를 복구한다")
    void should_expire_reserved_reservations_and_restore_stock() {
        ProductEntity product = productCommandJpaRepository.saveAndFlush(ProductEntity.builder()
                .productName("테스트 상품")
                .price(new BigDecimal("10000"))
                .stock(10)
                .status(ProductStatus.ON_SALE)
                .useYn(true)
                .build());

        adapter.reserve(
                "reservation-4",
                4L,
                List.of(new StockReservationItemPayload(product.getProductId(), 2))
        );

        StockReservationEntity reservation = stockReservationJpaRepository.findByReservationId("reservation-4").orElseThrow();
        reserveExpired(reservation, Instant.now().minusSeconds(1));

        int expiredCount = adapter.expireReservations(Instant.now());

        ProductEntity reloadedProduct = productCommandJpaRepository.findById(product.getProductId()).orElseThrow();
        StockReservationEntity expiredReservation = stockReservationJpaRepository.findByReservationId("reservation-4").orElseThrow();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(reloadedProduct.getStock()).isEqualTo(10);
        assertThat(expiredReservation.getStatus()).isEqualTo(StockReservationStatus.EXPIRED);
    }

    /**
     * 테스트에서 예약 만료 시각을 강제로 당겨 만료 대상을 만든다.
     */
    private void reserveExpired(StockReservationEntity reservation, Instant expiresAt) {
        try {
            java.lang.reflect.Field field = StockReservationEntity.class.getDeclaredField("expiresAt");
            field.setAccessible(true);
            field.set(reservation, expiresAt);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("expiresAt 조작에 실패했습니다.", e);
        }
    }
}
