-- =====================================================
-- order_db schema (v1 - e2e minimal)
-- =====================================================

-- 주문 마스터
CREATE TABLE IF NOT EXISTS orders (
                                      id BIGSERIAL PRIMARY KEY,
                                      order_number      VARCHAR(32) NOT NULL UNIQUE,     -- ORD-YYYYMMDD-000123
    user_id           BIGINT NOT NULL,
    status            VARCHAR(20) NOT NULL,            -- CREATED, PENDING_PAYMENT, PAID, CANCELLED
    currency          CHAR(3) NOT NULL DEFAULT 'KRW',

    subtotal_amount   NUMERIC(18,2) NOT NULL,
    discount_amount   NUMERIC(18,2) NOT NULL DEFAULT 0,
    shipping_fee      NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_amount      NUMERIC(18,2) NOT NULL,

    receiver_name     VARCHAR(80) NOT NULL,
    receiver_phone    VARCHAR(32) NOT NULL,
    shipping_postcode VARCHAR(10) NOT NULL,
    shipping_address1 VARCHAR(255) NOT NULL,
    shipping_address2 VARCHAR(255),
    memo              VARCHAR(255),

    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by bigint,
    updated_by bigint,

    CONSTRAINT chk_orders_status CHECK (
                                           status IN ('CREATED','PENDING_PAYMENT','PAID','CANCELLED')
    ),
    CONSTRAINT chk_amounts_nonneg CHECK (
                                            subtotal_amount >= 0 AND discount_amount >= 0 AND shipping_fee >= 0 AND total_amount >= 0
                                        )
    );

CREATE INDEX IF NOT EXISTS idx_orders_user_created
    ON orders (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_orders_status_created
    ON orders (status, created_at DESC);

-- 주문 품목
CREATE TABLE IF NOT EXISTS order_item (
                                          id BIGSERIAL PRIMARY KEY,
                                          order_id     BIGINT NOT NULL,
                                          product_id   BIGINT NOT NULL,
                                          product_name VARCHAR(200) NOT NULL,       -- 스냅샷
    unit_price   NUMERIC(18,2) NOT NULL,
    qty          INTEGER NOT NULL CHECK (qty > 0),
    line_amount  NUMERIC(18,2) NOT NULL,

    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by bigint,
    updated_by bigint,

    CONSTRAINT fk_order_item_order
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,

    CONSTRAINT chk_order_item_amounts CHECK (
                                                unit_price >= 0 AND line_amount >= 0
                                            )
    );

CREATE INDEX IF NOT EXISTS idx_order_item_order
    ON order_item (order_id);

CREATE INDEX IF NOT EXISTS idx_order_item_product
    ON order_item (product_id);

-- (선택) 주문번호 시퀀스/함수
CREATE SEQUENCE IF NOT EXISTS order_no_seq;

CREATE OR REPLACE FUNCTION next_order_number()
RETURNS varchar AS $$
BEGIN
RETURN 'ORD-' || to_char(current_date,'YYYYMMDD') || '-' ||
       lpad(nextval('order_no_seq')::text,6,'0');
END;
$$ LANGUAGE plpgsql;


CREATE TABLE IF NOT EXISTS order_status_history (
                                                    id BIGSERIAL PRIMARY KEY,
                                                    order_id    BIGINT NOT NULL,
                                                    from_status VARCHAR(20),
    to_status   VARCHAR(20) NOT NULL,
    reason      VARCHAR(255),
    changed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    changed_by  BIGINT,

    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by bigint,
    updated_by bigint,

    CONSTRAINT fk_status_hist_order
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,

    CONSTRAINT chk_status_hist_to CHECK (
                                            to_status IN ('CREATED','PENDING_PAYMENT','PAID','CANCELLED')
    )
    );

CREATE INDEX IF NOT EXISTS idx_status_hist_order
    ON order_status_history (order_id, changed_at);
