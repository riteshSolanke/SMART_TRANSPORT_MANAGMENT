ALTER TABLE tickets
    ADD COLUMN schedule_id BIGINT NULL AFTER route_id,
    ADD COLUMN service_date DATE NULL AFTER destination_stop_id,
    ADD COLUMN departure_time TIME NULL AFTER service_date,
    ADD COLUMN passenger_count INT NOT NULL DEFAULT 1 AFTER departure_time,
    ADD COLUMN unit_fare DECIMAL(10,2) NULL AFTER passenger_count,
    ADD COLUMN assignment_id BIGINT NULL AFTER unit_fare,
    ADD COLUMN vehicle_id BIGINT NULL AFTER assignment_id,
    ADD COLUMN idempotency_key VARCHAR(64) NULL AFTER vehicle_id,
    ADD COLUMN request_hash VARCHAR(64) NULL AFTER idempotency_key,
    ADD CONSTRAINT uk_ticket_user_idempotency
        UNIQUE (user_id, idempotency_key),
    ADD INDEX idx_ticket_inventory
        (route_id, schedule_id, service_date, status),
    ADD INDEX idx_ticket_user_booked
        (user_id, booked_at);
