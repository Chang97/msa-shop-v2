package com.msashop.product.adapter.out.persistence.adapter;

import com.msashop.common.event.payload.StockReservationItemPayload;
import com.msashop.product.adapter.out.persistence.entity.ProductEntity;
import com.msashop.product.adapter.out.persistence.entity.StockReservationEntity;
import com.msashop.product.adapter.out.persistence.repo.ProductCommandJpaRepository;
import com.msashop.product.adapter.out.persistence.repo.StockReservationJpaRepository;
import com.msashop.product.config.JpaAuditConfig;
import com.msashop.product.domain.model.ProductStatus;
import com.msashop.product.domain.model.StockReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({
        StockReservationPersistenceAdapter.class,
        JpaAuditConfig.class
})
class StockReservationPersistenceAdapterTest {

    @Autowired
    private StockReservationPersistenceAdapter adapter;

    @Autowired
    private ProductCommandJpaRepository productCommandJpaRepository;

    @Autowired
    private StockReservationJpaRepository stockReservationJpaRepository;

    /**
     * 목적:
     * - reserve() 호출 시 실제 product.stock이 감소하는지
     * - stock_reservation row가 RESERVED 상태로 생성되는지
     *
     * 기대값:
     * - 상품 재고가 예약 수량만큼 감소한다
     * - reservationId 기준 row가 생성되고 상태는 RESERVED다
     */
    @Test
    @DisplayName("reserve 호출 시 재고를 감소시키고 예약 row를 생성한다")
    void should_decrease_stock_and_create_reservation_rows_when_reserve_is_called() {
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
        List<StockReservationEntity> reservations = stockReservationJpaRepository.findByReservationId("reservation-1");

        assertThat(reloadedProduct.getStock()).isEqualTo(7);
        assertThat(reservations).hasSize(1);
        assertThat(reservations.get(0).getOrderId()).isEqualTo(1L);
        assertThat(reservations.get(0).getProductId()).isEqualTo(product.getProductId());
        assertThat(reservations.get(0).getQuantity()).isEqualTo(3);
        assertThat(reservations.get(0).getStatus()).isEqualTo(StockReservationStatus.RESERVED);
    }

    /**
     * 목적:
     * - release() 호출 시 RESERVED 예약이 RELEASED로 바뀌고
     *   감소됐던 재고가 복구되는지 확인한다
     *
     * 기대값:
     * - 상품 재고가 원래 값으로 복구된다
     * - 예약 상태가 RELEASED가 된다
     */
    @Test
    @DisplayName("release 호출 시 재고를 복구하고 예약 상태를 RELEASED로 변경한다")
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
        List<StockReservationEntity> reservations = stockReservationJpaRepository.findByReservationId("reservation-2");

        assertThat(reloadedProduct.getStock()).isEqualTo(10);
        assertThat(reservations).hasSize(1);
        assertThat(reservations.get(0).getStatus()).isEqualTo(StockReservationStatus.RELEASED);
    }

    /**
     * 목적:
     * - confirm() 호출 시 예약이 최종 확정 상태로 바뀌는지 확인한다
     *
     * 기대값:
     * - stock은 추가로 감소하지 않는다
     * - 예약 상태만 CONFIRMED로 바뀐다
     */
    @Test
    @DisplayName("confirm 호출 시 예약 상태를 CONFIRMED로 변경한다")
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
        List<StockReservationEntity> reservations = stockReservationJpaRepository.findByReservationId("reservation-3");

        assertThat(reloadedProduct.getStock()).isEqualTo(8);
        assertThat(reservations).hasSize(1);
        assertThat(reservations.get(0).getStatus()).isEqualTo(StockReservationStatus.CONFIRMED);
    }
}