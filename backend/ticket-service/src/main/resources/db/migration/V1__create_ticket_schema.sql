CREATE TABLE IF NOT EXISTS tickets (
    ticket_id BIGINT NOT NULL AUTO_INCREMENT,
    pnr_number VARCHAR(20) NOT NULL,
    user_id BIGINT NOT NULL,
    route_id BIGINT NOT NULL,
    source_stop_id BIGINT NOT NULL,
    destination_stop_id BIGINT NOT NULL,
    fare_amount DECIMAL(10,2) NOT NULL,
    status ENUM('BOOKED','CANCELLED','EXPIRED','USED') NOT NULL,
    booked_at DATETIME(6) NOT NULL,
    cancelled_at DATETIME(6),
    travel_date DATETIME(6),
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (ticket_id),
    CONSTRAINT uk_ticket_pnr UNIQUE (pnr_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
