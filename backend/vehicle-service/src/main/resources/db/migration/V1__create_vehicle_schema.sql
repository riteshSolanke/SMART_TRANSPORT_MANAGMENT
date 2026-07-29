CREATE TABLE IF NOT EXISTS vehicles (
    vehicle_id BIGINT NOT NULL AUTO_INCREMENT,
    vehicle_number VARCHAR(50) NOT NULL,
    capacity INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    active BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (vehicle_id),
    CONSTRAINT uk_vehicle_number UNIQUE (vehicle_number),
    CONSTRAINT chk_vehicle_capacity CHECK (capacity > 0),
    INDEX idx_vehicle_active_status (active, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS vehicle_assignments (
    assignment_id BIGINT NOT NULL AUTO_INCREMENT,
    vehicle_id BIGINT NOT NULL,
    route_id BIGINT NOT NULL,
    schedule_id BIGINT NOT NULL,
    service_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    assigned_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6),
    PRIMARY KEY (assignment_id),
    INDEX idx_assignment_vehicle_date_status (vehicle_id, service_date, status),
    INDEX idx_assignment_schedule_date_status (route_id, schedule_id, service_date, status),
    CONSTRAINT fk_assignment_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS vehicle_locations (
    location_id BIGINT NOT NULL AUTO_INCREMENT,
    vehicle_id BIGINT NOT NULL,
    latitude DECIMAL(9,6) NOT NULL,
    longitude DECIMAL(9,6) NOT NULL,
    speed_kph DECIMAL(6,2),
    recorded_at DATETIME(6) NOT NULL,
    PRIMARY KEY (location_id),
    INDEX idx_location_vehicle_recorded (vehicle_id, recorded_at),
    CONSTRAINT fk_location_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
