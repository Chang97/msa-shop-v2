CREATE TABLE IF NOT EXISTS stock_reservation (
     stock_reservation_id BIGSERIAL PRIMARY KEY,
     reservation_id VARCHAR(64) NOT NULL,
     order_id BIGINT NOT NULL,
     product_id BIGINT NOT NULL,
     quantity INT NOT NULL CHECK (quantity > 0),
     status VARCHAR(20) NOT NULL,
     expires_at TIMESTAMPTZ,
     created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
     updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_stock_reservation_order
    ON stock_reservation (order_id);

CREATE INDEX IF NOT EXISTS idx_stock_reservation_reservation
    ON stock_reservation (reservation_id);

CREATE INDEX IF NOT EXISTS idx_stock_reservation_status
    ON stock_reservation (status);

CREATE UNIQUE INDEX IF NOT EXISTS ux_stock_reservation_reservation_product
    ON stock_reservation (reservation_id, product_id);

COMMENT ON TABLE stock_reservation IS '주문 결제 saga에서 사용하는 예약 재고 테이블';
COMMENT ON COLUMN stock_reservation.reservation_id IS '한 주문의 예약 묶음을 식별하는 reservation id';
COMMENT ON COLUMN stock_reservation.order_id IS '주문 id';
COMMENT ON COLUMN stock_reservation.product_id IS '상품 id';
COMMENT ON COLUMN stock_reservation.quantity IS '예약 수량';
COMMENT ON COLUMN stock_reservation.status IS 'RESERVED, CONFIRMED, RELEASED, EXPIRED';
COMMENT ON COLUMN stock_reservation.expires_at IS '예약 만료 시각';