ALTER TABLE tickets
    MODIFY status ENUM(
        'PENDING_PAYMENT',
        'BOOKED',
        'CANCELLED',
        'EXPIRED',
        'USED',
        'PAYMENT_FAILED'
    ) NOT NULL,
    ADD COLUMN payment_expires_at DATETIME(6) NULL AFTER request_hash,
    ADD COLUMN payment_id BIGINT NULL AFTER payment_expires_at,
    ADD COLUMN paid_at DATETIME(6) NULL AFTER payment_id,
    ADD INDEX idx_ticket_payment_hold
        (route_id, schedule_id, service_date, status, payment_expires_at),
    ADD INDEX idx_ticket_payment_id (payment_id);
