package com.msashop.product.adapter.out.persistence.entity;

import com.msashop.product.domain.model.StockReservationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 주문 결제 saga 동안 임시로 잡아둔 재고를 표현하는 엔티티.
 *
 * reservationId:
 * 한 주문에 포함된 여러 상품 예약 row를 같은 묶음으로 추적하기 위한 식별자
 *
 * status:
 * RESERVED  - 재고를 선점했고 아직 결제 결과를 기다리는 상태
 * CONFIRMED - 결제가 성공해서 예약을 최종 확정한 상태
 * RELEASED  - 결제가 실패해서 재고를 다시 돌려준 상태
 */
@Entity
@Table(name = "stock_reservation")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_reservation_id")
    private Long stockReservationId;

    @Column(name = "reservation_id", nullable = false, length = 64)
    private String reservationId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StockReservationStatus status;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public void confirm() {
        if (this.status == StockReservationStatus.RESERVED) {
            this.status = StockReservationStatus.CONFIRMED;
        }
    }

    public void release() {
        if (this.status == StockReservationStatus.RESERVED) {
            this.status = StockReservationStatus.RELEASED;
        }
    }
}