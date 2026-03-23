ALTER TABLE processed_event
    ALTER COLUMN processed_at DROP NOT NULL;

ALTER TABLE processed_event
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'PROCESSED';

ALTER TABLE processed_event
    ADD COLUMN IF NOT EXISTS locked_by VARCHAR(100);

ALTER TABLE processed_event
    ADD COLUMN IF NOT EXISTS locked_at TIMESTAMPTZ;

ALTER TABLE processed_event
    ADD COLUMN IF NOT EXISTS last_error TEXT;

ALTER TABLE processed_event
    DROP CONSTRAINT IF EXISTS chk_processed_event_status;

ALTER TABLE processed_event
    ADD CONSTRAINT chk_processed_event_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'PROCESSED'));

CREATE INDEX IF NOT EXISTS idx_processed_event_claim
    ON processed_event (consumer_group, status, locked_at);

COMMENT ON TABLE processed_event IS 'Kafka consumer가 처리한 이벤트 이력 및 처리권(claim) 정보를 저장하는 inbox 테이블';

COMMENT ON COLUMN processed_event.status IS '이벤트 처리 상태. PENDING: 재처리 대기, PROCESSING: 현재 처리 중, PROCESSED: 처리 완료';
COMMENT ON COLUMN processed_event.locked_by IS '현재 이벤트 처리권을 선점한 worker 식별자';
COMMENT ON COLUMN processed_event.locked_at IS 'worker가 이벤트 처리권을 선점한 시각';
COMMENT ON COLUMN processed_event.last_error IS '마지막 처리 실패 사유';
COMMENT ON COLUMN processed_event.processed_at IS '이벤트 처리가 최종 완료된 시각';