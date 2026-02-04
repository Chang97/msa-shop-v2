-- =====================================================
-- payment_db schema (v1 - e2e minimal)
-- 목적: 결제 승인(모킹) + 멱등 처리 + 주문 상태 전이 트리거용 기록
-- =====================================================

CREATE TABLE IF NOT EXISTS payment_transaction (
                                                   payment_id        BIGSERIAL PRIMARY KEY,

    -- MSA boundary: store order_id as a value (no FK)
                                                   order_id          BIGINT NOT NULL,
                                                   user_id           BIGINT NOT NULL, -- 승인 요청자(게이트웨이에서 온 X-User-Id)

    -- amount (order_db와 타입 통일)
                                                   amount            NUMERIC(18,2) NOT NULL CHECK (amount >= 0),
    currency          CHAR(3) NOT NULL DEFAULT 'KRW',

    -- 멱등키 (approve 재시도 대비)
    idempotency_key   VARCHAR(80) NOT NULL,

    -- fake provider (추적용, 선택)
    provider          VARCHAR(50) NOT NULL DEFAULT 'FAKE',
    provider_tx_id    VARCHAR(100),

    -- lifecycle
    status            VARCHAR(30) NOT NULL,
    requested_at      timestamptz NOT NULL DEFAULT now(),
    approved_at       timestamptz,
    failed_at         timestamptz,
    fail_reason       TEXT,

    -- audit
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    created_by        BIGINT,
    updated_by        BIGINT,

    CONSTRAINT chk_payment_status CHECK (
                                            status IN ('PENDING','APPROVED','FAILED')
    )
    );

-- =========================
-- indexes / constraints
-- =========================

-- 멱등 보장 (동일 key로 들어오면 기존 row 반환)
CREATE UNIQUE INDEX IF NOT EXISTS ux_payment_tx_idempotency
    ON payment_transaction(idempotency_key);

-- 주문 기준 조회(주문별 결제 조회/검증)
CREATE INDEX IF NOT EXISTS ix_payment_tx_order_id
    ON payment_transaction(order_id);

-- (선택) provider tx id 유니크(있을 때만)
CREATE UNIQUE INDEX IF NOT EXISTS ux_payment_tx_provider_tx
    ON payment_transaction(provider, provider_tx_id)
    WHERE provider_tx_id IS NOT NULL;

-- =========================
-- comments (선택)
-- =========================
COMMENT ON TABLE payment_transaction IS '결제 트랜잭션(모킹). 멱등키 기반 승인 기록';
COMMENT ON COLUMN payment_transaction.idempotency_key IS 'approve 멱등 처리 키(UNIQUE)';
COMMENT ON COLUMN payment_transaction.status IS 'PENDING/APPROVED/FAILED';
