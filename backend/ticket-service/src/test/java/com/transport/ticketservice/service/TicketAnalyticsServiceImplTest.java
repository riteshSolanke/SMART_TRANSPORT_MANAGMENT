package com.transport.ticketservice.service;

import com.transport.ticketservice.enums.TicketStatus;
import com.transport.ticketservice.repository.TicketRepository;
import com.transport.ticketservice.repository.projection.RouteUsageAnalyticsProjection;
import com.transport.ticketservice.repository.projection.TicketStatusAnalyticsProjection;
import com.transport.ticketservice.service.impl.TicketAnalyticsServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TicketAnalyticsServiceImplTest {
    @Test
    void combinesConfirmedStatusesAndRouteUsage() {
        TicketRepository repository = mock(TicketRepository.class);
        List<TicketStatusAnalyticsProjection> statusRows = List.of(
                status(TicketStatus.BOOKED, 2, 5, "100.00"),
                status(TicketStatus.USED, 1, 2, "40.00"),
                status(TicketStatus.CANCELLED, 1, 1, "20.00"));
        List<RouteUsageAnalyticsProjection> routeRows =
                List.of(route(10L, 3, 7, "140.00"));
        when(repository.summarizeByStatus(FROM, TO))
                .thenReturn(statusRows);
        when(repository.summarizeConfirmedRouteUsage(
                org.mockito.ArgumentMatchers.eq(FROM),
                org.mockito.ArgumentMatchers.eq(TO),
                anyCollection()))
                .thenReturn(routeRows);

        var response =
                new TicketAnalyticsServiceImpl(repository).summarize(FROM, TO);

        assertThat(response.getTotalTicketRecords()).isEqualTo(4);
        assertThat(response.getConfirmedTickets()).isEqualTo(3);
        assertThat(response.getTotalPassengers()).isEqualTo(7);
        assertThat(response.getCancelledTickets()).isEqualTo(1);
        assertThat(response.getConfirmedFareValue())
                .isEqualByComparingTo("140.00");
        assertThat(response.getRouteUsage()).hasSize(1);
    }

    private TicketStatusAnalyticsProjection status(
            TicketStatus status,
            long tickets,
            long passengers,
            String fare) {
        TicketStatusAnalyticsProjection row =
                mock(TicketStatusAnalyticsProjection.class);
        when(row.getStatus()).thenReturn(status);
        when(row.getTicketCount()).thenReturn(tickets);
        when(row.getPassengerCount()).thenReturn(passengers);
        when(row.getFareTotal()).thenReturn(new BigDecimal(fare));
        return row;
    }

    private RouteUsageAnalyticsProjection route(
            Long routeId, long tickets, long passengers, String fare) {
        RouteUsageAnalyticsProjection row =
                mock(RouteUsageAnalyticsProjection.class);
        when(row.getRouteId()).thenReturn(routeId);
        when(row.getTicketCount()).thenReturn(tickets);
        when(row.getPassengerCount()).thenReturn(passengers);
        when(row.getFareTotal()).thenReturn(new BigDecimal(fare));
        return row;
    }

    private static final LocalDate FROM =
            LocalDate.of(2026, 7, 1);
    private static final LocalDate TO =
            LocalDate.of(2026, 7, 31);
}
