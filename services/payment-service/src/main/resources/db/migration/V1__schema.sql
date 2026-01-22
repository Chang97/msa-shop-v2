-- =====================================================
-- payment-service (fake payment) - V1 schema
-- - DB: payment_db
-- - Strategy: store payment attempts + optional refunds (including partial)
-- - Audit columns: created_at/updated_at/created_by/updated_by
-- - No use_yn (payments are records, lifecycle via status)
-- =====================================================

-- -----------------------------
-- 1) Payment transaction
-- -----------------------------
CREATE TABLE IF NOT EXISTS payment_transaction (
  payment_id        BIGSERIAL PRIMARY KEY,

  -- MSA boundary: store order_id as a value (no FK to order_db)
  order_id          BIGINT NOT NULL,

  -- money
  amount            BIGINT NOT NULL,
  currency          VARCHAR(10) NOT NULL DEFAULT 'KRW',

  -- fake payment provider info
  provider          VARCHAR(50) NOT NULL DEFAULT 'FAKE',
  provider_tx_id    VARCHAR(100),                 -- e.g., generated uuid/string for tracing

  -- lifecycle
  status            VARCHAR(30) NOT NULL,         -- PENDING/APPROVED/FAILED/CANCELED/REFUNDED/PARTIALLY_REFUNDED
  requested_at      timestamptz NOT NULL DEFAULT now(),
  approved_at       timestamptz,
  failed_at         timestamptz,
  canceled_at       timestamptz,

  fail_reason       TEXT,

  -- audit
  created_at        timestamptz NOT NULL DEFAULT now(),
  updated_at        timestamptz NOT NULL DEFAULT now(),
  created_by        BIGINT,
  updated_by        BIGINT
);

-- helpful indexes
CREATE INDEX IF NOT EXISTS ix_payment_tx_order_id
  ON payment_transaction(order_id);

CREATE INDEX IF NOT EXISTS ix_payment_tx_status
  ON payment_transaction(status);

-- uniqueness for provider transaction id when present
CREATE UNIQUE INDEX IF NOT EXISTS ux_payment_tx_provider_tx
  ON payment_transaction(provider, provider_tx_id)
  WHERE provider_tx_id IS NOT NULL;

-- -----------------------------
-- 2) Refund / cancel (supports partial refunds)
-- -----------------------------
CREATE TABLE IF NOT EXISTS payment_refund (
  refund_id         BIGSERIAL PRIMARY KEY,
  payment_id        BIGINT NOT NULL REFERENCES payment_transaction(payment_id) ON DELETE RESTRICT,

  refund_amount     BIGINT NOT NULL,
  reason            TEXT,

  -- lifecycle
  status            VARCHAR(30) NOT NULL DEFAULT 'REQUESTED', -- REQUESTED/APPROVED/REJECTED
  requested_at      timestamptz NOT NULL DEFAULT now(),
  approved_at       timestamptz,

  -- audit
  created_at        timestamptz NOT NULL DEFAULT now(),
  updated_at        timestamptz NOT NULL DEFAULT now(),
  created_by        BIGINT,
  updated_by        BIGINT
);

CREATE INDEX IF NOT EXISTS ix_payment_refund_payment_id
  ON payment_refund(payment_id);

CREATE INDEX IF NOT EXISTS ix_payment_refund_status
  ON payment_refund(status);

-- -----------------------------
-- 3) Outbox (optional but recommended for MSA integration)
--    - publish events to order-service: PAYMENT_APPROVED, PAYMENT_FAILED, PAYMENT_REFUNDED, etc.
-- -----------------------------
CREATE TABLE IF NOT EXISTS payment_outbox (
  event_id          BIGSERIAL PRIMARY KEY,

  aggregate_type    VARCHAR(50) NOT NULL DEFAULT 'PAYMENT',
  aggregate_id      BIGINT NOT NULL, -- payment_id
  event_type        VARCHAR(100) NOT NULL,
  payload           JSONB NOT NULL,

  status            VARCHAR(30) NOT NULL DEFAULT 'NEW', -- NEW/SENT/FAILED
  created_at        timestamptz NOT NULL DEFAULT now(),
  updated_at        timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_payment_outbox_status
  ON payment_outbox(status);

CREATE INDEX IF NOT EXISTS ix_payment_outbox_agg
  ON payment_outbox(aggregate_type, aggregate_id);

-- comments
COMMENT ON TABLE payment_transaction IS '테이블: payment_transaction';
COMMENT ON COLUMN payment_transaction.payment_id IS 'payment id';
COMMENT ON COLUMN payment_transaction.order_id IS 'order id';
COMMENT ON COLUMN payment_transaction.amount IS 'amount';
COMMENT ON COLUMN payment_transaction.currency IS 'currency';
COMMENT ON COLUMN payment_transaction.provider IS 'provider';
COMMENT ON COLUMN payment_transaction.provider_tx_id IS 'provider tx id';
COMMENT ON COLUMN payment_transaction.status IS 'status';
COMMENT ON COLUMN payment_transaction.requested_at IS 'requested at';
COMMENT ON COLUMN payment_transaction.approved_at IS 'approved at';
COMMENT ON COLUMN payment_transaction.failed_at IS 'failed at';
COMMENT ON COLUMN payment_transaction.canceled_at IS 'canceled at';
COMMENT ON COLUMN payment_transaction.fail_reason IS 'fail reason';
COMMENT ON COLUMN payment_transaction.created_at IS 'created at';
COMMENT ON COLUMN payment_transaction.updated_at IS 'updated at';
COMMENT ON COLUMN payment_transaction.created_by IS 'created by';
COMMENT ON COLUMN payment_transaction.updated_by IS 'updated by';
COMMENT ON TABLE payment_refund IS '테이블: payment_refund';
COMMENT ON COLUMN payment_refund.refund_id IS 'refund id';
COMMENT ON COLUMN payment_refund.payment_id IS 'payment id';
COMMENT ON COLUMN payment_refund.refund_amount IS 'refund amount';
COMMENT ON COLUMN payment_refund.reason IS 'reason';
COMMENT ON COLUMN payment_refund.status IS 'status';
COMMENT ON COLUMN payment_refund.requested_at IS 'requested at';
COMMENT ON COLUMN payment_refund.approved_at IS 'approved at';
COMMENT ON COLUMN payment_refund.created_at IS 'created at';
COMMENT ON COLUMN payment_refund.updated_at IS 'updated at';
COMMENT ON COLUMN payment_refund.created_by IS 'created by';
COMMENT ON COLUMN payment_refund.updated_by IS 'updated by';
COMMENT ON TABLE payment_outbox IS '테이블: payment_outbox';
COMMENT ON COLUMN payment_outbox.event_id IS 'event id';
COMMENT ON COLUMN payment_outbox.aggregate_type IS 'aggregate type';
COMMENT ON COLUMN payment_outbox.aggregate_id IS 'aggregate id';
COMMENT ON COLUMN payment_outbox.event_type IS 'event type';
COMMENT ON COLUMN payment_outbox.payload IS 'payload';
COMMENT ON COLUMN payment_outbox.status IS 'status';
COMMENT ON COLUMN payment_outbox.created_at IS 'created at';
COMMENT ON COLUMN payment_outbox.updated_at IS 'updated at';
