package com.transport.ticketservice.service;

import com.transport.ticketservice.client.RouteServiceClient;
import com.transport.ticketservice.client.VehicleServiceClient;
import com.transport.ticketservice.dto.request.TicketRequestDto;
import com.transport.ticketservice.dto.response.TicketResponseDto;
import com.transport.ticketservice.entity.Ticket;
import com.transport.ticketservice.enums.TicketStatus;
import com.transport.ticketservice.mapper.TicketMapper;
import com.transport.ticketservice.repository.TicketRepository;
import com.transport.ticketservice.service.impl.TicketServiceImpl;
import com.transport.ticketservice.util.BookingRequestHasher;
import com.transport.ticketservice.util.TicketNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceAuthorizationTest {
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private RouteServiceClient routeServiceClient;
    @Mock
    private VehicleServiceClient vehicleServiceClient;
    @Mock
    private TicketNumberGenerator ticketNumberGenerator;
    private TicketServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TicketServiceImpl(
                ticketRepository,
                new TicketMapper(),
                routeServiceClient,
                vehicleServiceClient,
                ticketNumberGenerator,
                new BookingRequestHasher());
    }

    @Test
    void passengerCannotBookForAnotherUser() {
        TicketRequestDto request = new TicketRequestDto();
        request.setUserId(99L);

        assertThatThrownBy(() -> service.bookTicket(
                request, "request-123", 42L, false))
                .isInstanceOf(AccessDeniedException.class);

        verify(routeServiceClient, never()).getFare(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
        verify(ticketRepository, never()).save(
                org.mockito.ArgumentMatchers.any(Ticket.class));
    }

    @Test
    void passengerCannotReadAnotherUsersTicket() {
        Ticket ticket = ticketOwnedBy(99L);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() ->
                service.getTicketById(1L, 42L, false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void passengerCannotCancelAnotherUsersTicket() {
        Ticket ticket = ticketOwnedBy(99L);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() ->
                service.cancelTicket(1L, 42L, false))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.BOOKED);
        verify(ticketRepository, never()).save(ticket);
    }

    @Test
    void privilegedUserCanReadAnyTicket() {
        Ticket ticket = ticketOwnedBy(99L);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        TicketResponseDto response =
                service.getTicketById(1L, 7L, true);

        assertThat(response.getUserId()).isEqualTo(99L);
    }

    @Test
    void passengerCanReadOwnTicket() {
        Ticket ticket = ticketOwnedBy(42L);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        TicketResponseDto response =
                service.getTicketById(1L, 42L, false);

        assertThat(response.getTicketId()).isEqualTo(1L);
    }

    private Ticket ticketOwnedBy(Long userId) {
        return Ticket.builder()
                .ticketId(1L)
                .pnrNumber("PNR0000001")
                .userId(userId)
                .routeId(10L)
                .sourceStopId(100L)
                .destinationStopId(200L)
                .passengerCount(1)
                .status(TicketStatus.BOOKED)
                .build();
    }
}
