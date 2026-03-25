CREATE TABLE IF NOT EXISTS outbox_event (
    outbox_event_id BIGSERIAL PRIMARY KEY,
    event_id        VARCHAR(36)  NOT NULL UNIQUE,
    topic           VARCHAR(200) NOT NULL,
    event_key       VARCHAR(200) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    saga_id         VARCHAR(100),
    correlation_id  VARCHAR(100),
    causation_id    VARCHAR(100),
    payload_json    JSONB        NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count     INTEGER      NOT NULL DEFAULT 0,
    published_at    TIMESTAMPTZ,
    last_error      TEXT,
    locked_by       VARCHAR(100),
    locked_at       TIMESTAMPTZ,
    next_retry_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_outbox_event_claim
    ON outbox_event (status, locked_at, outbox_event_id);

CREATE INDEX IF NOT EXISTS idx_outbox_event_retry
    ON outbox_event (status, next_retry_at, outbox_event_id);

CREATE TABLE IF NOT EXISTS processed_event (
    processed_event_id BIGSERIAL PRIMARY KEY,
    consumer_group     VARCHAR(150) NOT NULL,
    event_id           VARCHAR(36)  NOT NULL,
    event_type         VARCHAR(100) NOT NULL,
    topic              VARCHAR(200) NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PROCESSED',
    processed_at       TIMESTAMPTZ,
    locked_by          VARCHAR(100),
    locked_at          TIMESTAMPTZ,
    last_error         TEXT,
    CONSTRAINT ux_processed_event UNIQUE (consumer_group, event_id),
    CONSTRAINT chk_processed_event_status CHECK (status IN ('PENDING', 'PROCESSING', 'PROCESSED'))
);

CREATE INDEX IF NOT EXISTS idx_processed_event_claim
    ON processed_event (consumer_group, status, locked_at);

COMMENT ON TABLE outbox_event IS '비동기 도메인 이벤트 발행용 outbox 테이블';
COMMENT ON COLUMN outbox_event.status IS 'PENDING/PROCESSING/PUBLISHED/FAILED';
COMMENT ON COLUMN outbox_event.locked_by IS '현재 outbox row를 선점한 worker 식별자';
COMMENT ON COLUMN outbox_event.locked_at IS '현재 worker가 outbox row를 선점한 시각';
COMMENT ON COLUMN outbox_event.next_retry_at IS '다음 재시도 가능 시각';

COMMENT ON TABLE processed_event IS 'Kafka consumer 처리 이력 및 claim 정보를 저장하는 inbox 테이블';
COMMENT ON COLUMN processed_event.status IS 'PENDING/PROCESSING/PROCESSED';
COMMENT ON COLUMN processed_event.locked_by IS '현재 이벤트 처리권을 선점한 worker 식별자';
COMMENT ON COLUMN processed_event.locked_at IS '현재 worker가 이벤트 처리권을 선점한 시각';
COMMENT ON COLUMN processed_event.last_error IS '마지막 처리 실패 사유';
