ALTER TABLE outbox_event
    ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_outbox_event_retry
    ON outbox_event (status, next_retry_at, outbox_event_id);

COMMENT ON COLUMN outbox_event.next_retry_at IS '다음 재시도 가능 시각. null이면 즉시 재시도 가능';