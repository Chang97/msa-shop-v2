ALTER TABLE stock_reservation RENAME TO stock_reservation_legacy;

CREATE TABLE IF NOT EXISTS stock_reservation (
    stock_reservation_id BIGSERIAL PRIMARY KEY,
    reservation_id VARCHAR(64) NOT NULL,
    order_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT,
    updated_by BIGINT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_stock_reservation_reservation_id
    ON stock_reservation (reservation_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_stock_reservation_active_order
    ON stock_reservation (order_id)
    WHERE status IN ('RESERVED', 'CONFIRMED');

CREATE INDEX IF NOT EXISTS idx_stock_reservation_status
    ON stock_reservation (status);

CREATE INDEX IF NOT EXISTS idx_stock_reservation_expires_at
    ON stock_reservation (expires_at);

CREATE TABLE IF NOT EXISTS stock_reservation_item (
    stock_reservation_item_id BIGSERIAL PRIMARY KEY,
    stock_reservation_id BIGINT NOT NULL REFERENCES stock_reservation(stock_reservation_id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT,
    updated_by BIGINT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_stock_reservation_item_reservation_product
    ON stock_reservation_item (stock_reservation_id, product_id);

CREATE INDEX IF NOT EXISTS idx_stock_reservation_item_product
    ON stock_reservation_item (product_id);

INSERT INTO stock_reservation (
    reservation_id,
    order_id,
    status,
    expires_at,
    created_at,
    updated_at
)
SELECT
    legacy.reservation_id,
    MIN(legacy.order_id) AS order_id,
    CASE
        WHEN bool_or(legacy.status = 'CONFIRMED') THEN 'CONFIRMED'
        WHEN bool_or(legacy.status = 'RESERVED') THEN 'RESERVED'
        WHEN bool_or(legacy.status = 'RELEASED') THEN 'RELEASED'
        ELSE 'EXPIRED'
    END AS status,
    MIN(legacy.expires_at) AS expires_at,
    MIN(legacy.created_at) AS created_at,
    MAX(legacy.updated_at) AS updated_at
FROM stock_reservation_legacy legacy
GROUP BY legacy.reservation_id;

INSERT INTO stock_reservation_item (
    stock_reservation_id,
    product_id,
    quantity,
    created_at,
    updated_at
)
SELECT
    reservation.stock_reservation_id,
    legacy.product_id,
    legacy.quantity,
    legacy.created_at,
    legacy.updated_at
FROM stock_reservation_legacy legacy
JOIN stock_reservation reservation
  ON reservation.reservation_id = legacy.reservation_id;

DROP TABLE stock_reservation_legacy;

COMMENT ON TABLE stock_reservation IS '주문 단위 재고 예약 헤더 테이블';
COMMENT ON COLUMN stock_reservation.reservation_id IS '예약 묶음을 식별하는 고유 reservation id';
COMMENT ON COLUMN stock_reservation.order_id IS '주문 id';
COMMENT ON COLUMN stock_reservation.status IS 'RESERVED, CONFIRMED, RELEASED, EXPIRED';
COMMENT ON COLUMN stock_reservation.expires_at IS '예약 만료 시각';

COMMENT ON TABLE stock_reservation_item IS '예약 헤더에 속한 상품별 예약 수량 테이블';
COMMENT ON COLUMN stock_reservation_item.stock_reservation_id IS '예약 헤더 id';
COMMENT ON COLUMN stock_reservation_item.product_id IS '상품 id';
COMMENT ON COLUMN stock_reservation_item.quantity IS '예약 수량';
