package com.transport.ticketservice.service;

import com.transport.ticketservice.dto.response.TicketAnalyticsDto;

import java.time.LocalDate;

public interface TicketAnalyticsService {
    TicketAnalyticsDto summarize(LocalDate from, LocalDate to);
}
