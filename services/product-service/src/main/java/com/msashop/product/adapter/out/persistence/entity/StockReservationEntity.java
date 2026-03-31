package com.msashop.product.adapter.out.persistence.entity;

import com.msashop.product.domain.model.StockReservationStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 주문 단위 재고 예약 헤더 엔티티.
 */
@Entity
@Table(name = "stock_reservation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReservationEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_reservation_id")
    private Long stockReservationId;

    @Column(name = "reservation_id", nullable = false, length = 64, unique = true)
    private String reservationId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StockReservationStatus status;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @OneToMany(mappedBy = "stockReservation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final List<StockReservationItemEntity> items = new ArrayList<>();

    private StockReservationEntity(
            String reservationId,
            Long orderId,
            StockReservationStatus status,
            Instant expiresAt
    ) {
        this.reservationId = Objects.requireNonNull(reservationId, "reservationId");
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        this.status = Objects.requireNonNull(status, "status");
        this.expiresAt = expiresAt;
    }

    /**
     * 새 예약 헤더 엔티티를 만든다.
     */
    public static StockReservationEntity of(
            String reservationId,
            Long orderId,
            StockReservationStatus status,
            Instant expiresAt
    ) {
        return new StockReservationEntity(reservationId, orderId, status, expiresAt);
    }

    /**
     * 예약 헤더에 상품별 예약 수량 아이템을 추가한다.
     */
    public void addItem(Long productId, Integer quantity) {
        items.add(StockReservationItemEntity.of(this, productId, quantity));
    }

    /**
     * RESERVED 상태의 예약을 최종 확정한다.
     */
    public boolean confirm() {
        if (status != StockReservationStatus.RESERVED) {
            return false;
        }
        this.status = StockReservationStatus.CONFIRMED;
        this.expiresAt = null;
        return true;
    }

    /**
     * RESERVED 상태의 예약을 해제한다.
     */
    public boolean release() {
        if (status != StockReservationStatus.RESERVED) {
            return false;
        }
        this.status = StockReservationStatus.RELEASED;
        this.expiresAt = null;
        return true;
    }

    /**
     * RESERVED 상태의 예약을 만료 처리한다.
     */
    public boolean expire() {
        if (status != StockReservationStatus.RESERVED) {
            return false;
        }
        this.status = StockReservationStatus.EXPIRED;
        this.expiresAt = null;
        return true;
    }
}
