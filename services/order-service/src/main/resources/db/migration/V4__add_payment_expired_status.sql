ALTER TABLE orders
    DROP CONSTRAINT IF EXISTS orders_status_check;

ALTER TABLE orders
    ADD CONSTRAINT orders_status_check
        CHECK (
            status IN ('CREATED', 'PENDING_PAYMENT', 'PAYMENT_FAILED', 'PAYMENT_EXPIRED', 'PAID', 'CANCELLED')
        );

ALTER TABLE order_status_history
    DROP CONSTRAINT IF EXISTS order_status_history_to_status_check;

ALTER TABLE order_status_history
    ADD CONSTRAINT order_status_history_to_status_check
        CHECK (
            to_status IN ('CREATED', 'PENDING_PAYMENT', 'PAYMENT_FAILED', 'PAYMENT_EXPIRED', 'PAID', 'CANCELLED')
        );
