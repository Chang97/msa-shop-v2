COMMENT ON COLUMN outbox_event.locked_by IS '현재 outbox row를 선점하여 발행 중인 worker 식별자';

COMMENT ON COLUMN outbox_event.locked_at IS 'worker가 outbox row를 PROCESSING 상태로 선점한 시각';
