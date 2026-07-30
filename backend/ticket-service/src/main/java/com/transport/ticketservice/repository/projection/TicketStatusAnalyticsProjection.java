package com.transport.ticketservice.repository.projection;

import com.transport.ticketservice.enums.TicketStatus;

import java.math.BigDecimal;

public interface TicketStatusAnalyticsProjection {
    TicketStatus getStatus();

    Long getTicketCount();

    Long getPassengerCount();

    BigDecimal getFareTotal();
}
