ALTER TABLE outbox_event
    ADD COLUMN IF NOT EXISTS locked_by VARCHAR(100);

ALTER TABLE outbox_event
    ADD COLUMN IF NOT EXISTS locked_at TIMESTAMPTZ;

ALTER TABLE outbox_event
DROP CONSTRAINT IF EXISTS chk_outbox_status;

ALTER TABLE outbox_event
    ADD CONSTRAINT chk_outbox_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED'));

CREATE INDEX IF NOT EXISTS idx_outbox_event_claim
    ON outbox_event (status, locked_at, outbox_event_id);