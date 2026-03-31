package com.msashop.product.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 단위 재고 예약에 속한 상품별 예약 수량 엔티티.
 */
@Entity
@Table(
        name = "stock_reservation_item",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_stock_reservation_item_reservation_product",
                columnNames = {"stock_reservation_id", "product_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReservationItemEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_reservation_item_id")
    private Long stockReservationItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_reservation_id", nullable = false)
    private StockReservationEntity stockReservation;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    private StockReservationItemEntity(
            StockReservationEntity stockReservation,
            Long productId,
            Integer quantity
    ) {
        this.stockReservation = stockReservation;
        this.productId = productId;
        this.quantity = quantity;
    }

    /**
     * 예약 헤더에 연결된 상품별 수량 item을 만든다.
     */
    public static StockReservationItemEntity of(
            StockReservationEntity stockReservation,
            Long productId,
            Integer quantity
    ) {
        return new StockReservationItemEntity(stockReservation, productId, quantity);
    }
}
