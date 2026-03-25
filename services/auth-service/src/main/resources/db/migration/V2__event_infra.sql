CREATE TABLE IF NOT EXISTS outbox_event (
                                            outbox_event_id  BIGSERIAL PRIMARY KEY,
                                            event_id         VARCHAR(36)  NOT NULL UNIQUE,
    topic            VARCHAR(200) NOT NULL,
    event_key        VARCHAR(200) NOT NULL,
    event_type       VARCHAR(100) NOT NULL,
    aggregate_type   VARCHAR(100) NOT NULL,
    aggregate_id     VARCHAR(100) NOT NULL,
    saga_id          VARCHAR(100),
    correlation_id   VARCHAR(100),
    causation_id     VARCHAR(100),
    payload_json     JSONB        NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count      INTEGER      NOT NULL DEFAULT 0,
    published_at     TIMESTAMPTZ,
    last_error       TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING','PUBLISHED','FAILED'))
    );

CREATE INDEX IF NOT EXISTS idx_outbox_event_status_created
    ON outbox_event (status, created_at);

CREATE TABLE IF NOT EXISTS processed_event (
                                               processed_event_id BIGSERIAL PRIMARY KEY,
                                               consumer_group     VARCHAR(150) NOT NULL,
    event_id           VARCHAR(36)  NOT NULL,
    event_type         VARCHAR(100) NOT NULL,
    topic              VARCHAR(200) NOT NULL,
    processed_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ux_processed_event UNIQUE (consumer_group, event_id)
    );