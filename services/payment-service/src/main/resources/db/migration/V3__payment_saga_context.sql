ALTER TABLE payment_transaction
    ADD COLUMN IF NOT EXISTS reservation_id VARCHAR(64);

ALTER TABLE payment_transaction
    ADD COLUMN IF NOT EXISTS saga_id VARCHAR(100);

ALTER TABLE payment_transaction
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(100);

ALTER TABLE payment_transaction
    ADD COLUMN IF NOT EXISTS source_event_id VARCHAR(100);

ALTER TABLE payment_transaction
    DROP CONSTRAINT IF EXISTS chk_payment_status;

ALTER TABLE payment_transaction
    ADD CONSTRAINT chk_payment_status
        CHECK (status IN ('REQUESTED', 'APPROVAL_UNKNOWN', 'APPROVED', 'FAILED'));

CREATE INDEX IF NOT EXISTS ix_payment_tx_status_requested_at
    ON payment_transaction (status, requested_at);

COMMENT ON COLUMN payment_transaction.reservation_id IS 'product-service가 생성한 예약 id';
COMMENT ON COLUMN payment_transaction.saga_id IS '주문 결제 saga 식별자';
COMMENT ON COLUMN payment_transaction.correlation_id IS '같은 saga 체인 추적용 correlation id';
COMMENT ON COLUMN payment_transaction.source_event_id IS 'StockReserved 원본 이벤트 id';
COMMENT ON COLUMN payment_transaction.status IS 'REQUESTED/APPROVAL_UNKNOWN/APPROVED/FAILED';