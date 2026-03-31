ALTER TABLE orders
    DROP CONSTRAINT IF EXISTS chk_orders_status;

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_status CHECK (
        status IN ('CREATED', 'PENDING_PAYMENT', 'PAYMENT_FAILED', 'PAID', 'CANCELLED')
    );

ALTER TABLE order_status_history
    DROP CONSTRAINT IF EXISTS chk_status_hist_to;

ALTER TABLE order_status_history
    ADD CONSTRAINT chk_status_hist_to CHECK (
        to_status IN ('CREATED', 'PENDING_PAYMENT', 'PAYMENT_FAILED', 'PAID', 'CANCELLED')
    );
