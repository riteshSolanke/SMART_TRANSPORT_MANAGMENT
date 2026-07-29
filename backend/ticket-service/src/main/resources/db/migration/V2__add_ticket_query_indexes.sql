CREATE INDEX idx_ticket_user
    ON tickets(user_id);

CREATE INDEX idx_ticket_route_status
    ON tickets(route_id, status);

CREATE INDEX idx_ticket_travel_date
    ON tickets(travel_date);
