package com.transport.ticketservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class TicketAnalyticsDto {
    private LocalDate from;
    private LocalDate to;
    private long totalTicketRecords;
    private long confirmedTickets;
    private long totalPassengers;
    private long cancelledTickets;
    private long expiredTickets;
    private long paymentFailedTickets;
    private BigDecimal confirmedFareValue;
    private List<RouteUsageAnalyticsDto> routeUsage;
}
