CREATE TABLE IF NOT EXISTS routes (
    route_id BIGINT NOT NULL AUTO_INCREMENT,
    route_name VARCHAR(200) NOT NULL,
    start_point VARCHAR(200) NOT NULL,
    end_point VARCHAR(200) NOT NULL,
    active BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    PRIMARY KEY (route_id),
    INDEX idx_route_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS stops (
    stop_id BIGINT NOT NULL AUTO_INCREMENT,
    route_id BIGINT NOT NULL,
    stop_name VARCHAR(100) NOT NULL,
    sequence_order INT NOT NULL,
    distance_from_start DECIMAL(8,2) NOT NULL,
    PRIMARY KEY (stop_id),
    CONSTRAINT uk_route_sequence UNIQUE (route_id, sequence_order),
    CONSTRAINT fk_stops_route
        FOREIGN KEY (route_id) REFERENCES routes(route_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS schedules (
    schedule_id BIGINT NOT NULL AUTO_INCREMENT,
    route_id BIGINT NOT NULL,
    departure_time TIME NOT NULL,
    arrival_time TIME NOT NULL,
    days_of_week VARCHAR(30) NOT NULL,
    active BIT(1) NOT NULL,
    PRIMARY KEY (schedule_id),
    INDEX idx_schedule_route (route_id),
    CONSTRAINT fk_schedules_route
        FOREIGN KEY (route_id) REFERENCES routes(route_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
