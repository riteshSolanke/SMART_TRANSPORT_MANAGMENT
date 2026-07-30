package com.transport.ticketservice.repository.projection;

import java.math.BigDecimal;

public interface RouteUsageAnalyticsProjection {
    Long getRouteId();

    Long getTicketCount();

    Long getPassengerCount();

    BigDecimal getFareTotal();
}
