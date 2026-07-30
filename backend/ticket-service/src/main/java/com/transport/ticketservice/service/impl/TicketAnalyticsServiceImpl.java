package com.transport.ticketservice.service.impl;

import com.transport.ticketservice.dto.response.RouteUsageAnalyticsDto;
import com.transport.ticketservice.dto.response.TicketAnalyticsDto;
import com.transport.ticketservice.enums.TicketStatus;
import com.transport.ticketservice.repository.TicketRepository;
import com.transport.ticketservice.repository.projection.TicketStatusAnalyticsProjection;
import com.transport.ticketservice.service.TicketAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketAnalyticsServiceImpl implements TicketAnalyticsService {
    private static final EnumSet<TicketStatus> CONFIRMED =
            EnumSet.of(TicketStatus.BOOKED, TicketStatus.USED);

    private final TicketRepository ticketRepository;

    @Override
    @Transactional(readOnly = true)
    public TicketAnalyticsDto summarize(LocalDate from, LocalDate to) {
        validateRange(from, to);
        Map<TicketStatus, TicketStatusAnalyticsProjection> byStatus =
                ticketRepository.summarizeByStatus(from, to).stream()
                        .collect(Collectors.toMap(
                                TicketStatusAnalyticsProjection::getStatus,
                                Function.identity()));
        long totalRecords = byStatus.values().stream()
                .mapToLong(this::tickets).sum();
        long confirmedTickets = CONFIRMED.stream()
                .map(byStatus::get)
                .mapToLong(this::tickets).sum();
        long passengers = CONFIRMED.stream()
                .map(byStatus::get)
                .mapToLong(this::passengers).sum();
        BigDecimal confirmedFare = CONFIRMED.stream()
                .map(byStatus::get)
                .map(this::fare)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var routeUsage = ticketRepository
                .summarizeConfirmedRouteUsage(from, to, CONFIRMED)
                .stream()
                .map(row -> RouteUsageAnalyticsDto.builder()
                        .routeId(row.getRouteId())
                        .ticketCount(value(row.getTicketCount()))
                        .passengerCount(value(row.getPassengerCount()))
                        .fareValue(money(row.getFareTotal()))
                        .build())
                .toList();

        return TicketAnalyticsDto.builder()
                .from(from)
                .to(to)
                .totalTicketRecords(totalRecords)
                .confirmedTickets(confirmedTickets)
                .totalPassengers(passengers)
                .cancelledTickets(tickets(byStatus.get(TicketStatus.CANCELLED)))
                .expiredTickets(tickets(byStatus.get(TicketStatus.EXPIRED)))
                .paymentFailedTickets(
                        tickets(byStatus.get(TicketStatus.PAYMENT_FAILED)))
                .confirmedFareValue(confirmedFare)
                .routeUsage(routeUsage)
                .build();
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "A valid from/to date range is required");
        }
        if (ChronoUnit.DAYS.between(from, to) > 366) {
            throw new IllegalArgumentException(
                    "Analytics date range cannot exceed 366 days");
        }
    }

    private long tickets(TicketStatusAnalyticsProjection row) {
        return row == null ? 0 : value(row.getTicketCount());
    }

    private long passengers(TicketStatusAnalyticsProjection row) {
        return row == null ? 0 : value(row.getPassengerCount());
    }

    private BigDecimal fare(TicketStatusAnalyticsProjection row) {
        return row == null ? BigDecimal.ZERO : money(row.getFareTotal());
    }

    private long value(Long value) {
        return value == null ? 0 : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
