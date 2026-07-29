CREATE INDEX idx_stop_route_name_sequence
    ON stops(route_id, stop_name, sequence_order);

CREATE INDEX idx_schedule_route_active_departure
    ON schedules(route_id, active, departure_time);
