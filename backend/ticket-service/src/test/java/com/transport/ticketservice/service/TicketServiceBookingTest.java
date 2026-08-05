package com.transport.ticketservice.service;

import com.transport.ticketservice.client.RouteServiceClient;
import com.transport.ticketservice.client.VehicleServiceClient;
import com.transport.ticketservice.dto.request.TicketRequestDto;
import com.transport.ticketservice.dto.response.ApiResponseDto;
import com.transport.ticketservice.dto.response.FareResponseDto;
import com.transport.ticketservice.dto.response.SeatAvailabilityResponseDto;
import com.transport.ticketservice.dto.response.VehicleAvailabilityResponseDto;
import com.transport.ticketservice.entity.Ticket;
import com.transport.ticketservice.enums.TicketStatus;
import com.transport.ticketservice.exception.IdempotencyConflictException;
import com.transport.ticketservice.exception.InvalidTicketStateException;
import com.transport.ticketservice.exception.SeatUnavailableException;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceBookingTest {
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private RouteServiceClient routeServiceClient;
    @Mock
    private VehicleServiceClient vehicleServiceClient;
    @Mock
    private TicketNumberGenerator numberGenerator;
    private final BookingRequestHasher hasher = new BookingRequestHasher();
    private TicketServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TicketServiceImpl(
                ticketRepository,
                new TicketMapper(),
                routeServiceClient,
                vehicleServiceClient,
                numberGenerator,
                hasher);
        lenient().when(ticketRepository.save(any(Ticket.class)))
                .thenAnswer(invocation -> {
                    Ticket ticket = invocation.getArgument(0);
                    ticket.setTicketId(1L);
                    return ticket;
                });
    }

    @Test
    void booksSelectedScheduleAndCalculatesGroupFare() {
        TicketRequestDto request = request();
        stubDependencies(request, 40, 5);
        when(numberGenerator.generate()).thenReturn("PNR0000001");
        when(ticketRepository.existsByPnrNumber("PNR0000001"))
                .thenReturn(false);

        var response = service.bookTicket(
                request, "booking-123", 42L, false);

        assertThat(response.getScheduleId()).isEqualTo(20L);
        assertThat(response.getPassengerCount()).isEqualTo(2);
        assertThat(response.getUnitFare()).isEqualByComparingTo("25.00");
        assertThat(response.getFareAmount()).isEqualByComparingTo("50.00");
        assertThat(response.getAssignmentId()).isEqualTo(7L);
        assertThat(response.getVehicleId()).isEqualTo(8L);
        assertThat(response.getStatus())
                .isEqualTo(TicketStatus.PENDING_PAYMENT);
        assertThat(response.getPaymentExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void returnsExistingTicketForSameIdempotentRequest() {
        TicketRequestDto request = request();
        Ticket existing = existingTicket(request);
        existing.setRequestHash(hasher.hash(42L, request));
        when(ticketRepository.findByUserIdAndIdempotencyKey(
                42L, "booking-123")).thenReturn(Optional.of(existing));

        var response = service.bookTicket(
                request, "booking-123", 42L, false);

        assertThat(response.getPnrNumber()).isEqualTo("PNR0000001");
        verifyNoInteractions(routeServiceClient, vehicleServiceClient);
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void rejectsIdempotencyKeyReusedForDifferentPayload() {
        TicketRequestDto request = request();
        Ticket existing = existingTicket(request);
        existing.setRequestHash("different-hash");
        when(ticketRepository.findByUserIdAndIdempotencyKey(
                42L, "booking-123")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.bookTicket(
                request, "booking-123", 42L, false))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void rejectsBookingWithoutAssignedVehicle() {
        TicketRequestDto request = request();
        when(routeServiceClient.getFare(
                10L, 100L, 200L, 20L, "42", "PASSENGER"))
                .thenReturn(ApiResponseDto.success(fare()));
        VehicleAvailabilityResponseDto availability =
                availability(request, 0);
        availability.setAssigned(false);
        when(vehicleServiceClient.getAvailability(
                10L, 20L, request.getServiceDate(), "42", "PASSENGER"))
                .thenReturn(ApiResponseDto.success(availability));

        assertThatThrownBy(() -> service.bookTicket(
                request, "booking-123", 42L, false))
                .isInstanceOf(SeatUnavailableException.class);
    }

    @Test
    void rejectsBookingThatExceedsRemainingCapacity() {
        TicketRequestDto request = request();
        stubDependencies(request, 6, 5);

        assertThatThrownBy(() -> service.bookTicket(
                request, "booking-123", 42L, false))
                .isInstanceOf(SeatUnavailableException.class)
                .hasMessageContaining("Only 1 seat");
    }

    @Test
    void reportsRemainingSeatsForAssignedVehicle() {
        TicketRequestDto request = request();
        when(vehicleServiceClient.getAvailability(
                10L, 20L, request.getServiceDate(), "42", "PASSENGER"))
                .thenReturn(ApiResponseDto.success(
                        availability(request, 40)));
        when(ticketRepository.countReservedPassengers(
                eq(10L), eq(20L), eq(request.getServiceDate()),
                anyCollection(), eq(TicketStatus.PENDING_PAYMENT),
                any(LocalDateTime.class))).thenReturn(7L);

        SeatAvailabilityResponseDto response =
                service.getSeatAvailability(
                        10L, 20L, request.getServiceDate(), 42L);

        assertThat(response.isAssigned()).isTrue();
        assertThat(response.getCapacity()).isEqualTo(40);
        assertThat(response.getReservedSeats()).isEqualTo(7L);
        assertThat(response.getRemainingSeats()).isEqualTo(33L);
        assertThat(response.isAvailable()).isTrue();
    }

    @Test
    void reportsUnavailableWhenNoVehicleIsAssigned() {
        TicketRequestDto request = request();
        VehicleAvailabilityResponseDto availability =
                availability(request, 0);
        availability.setAssigned(false);
        when(vehicleServiceClient.getAvailability(
                10L, 20L, request.getServiceDate(), "42", "PASSENGER"))
                .thenReturn(ApiResponseDto.success(availability));

        SeatAvailabilityResponseDto response =
                service.getSeatAvailability(
                        10L, 20L, request.getServiceDate(), 42L);

        assertThat(response.isAssigned()).isFalse();
        assertThat(response.getRemainingSeats()).isZero();
        assertThat(response.isAvailable()).isFalse();
    }

    @Test
    void rejectsMismatchedFareResponse() {
        TicketRequestDto request = request();
        FareResponseDto fare = fare();
        fare.setScheduleId(999L);
        when(routeServiceClient.getFare(
                10L, 100L, 200L, 20L, "42", "PASSENGER"))
                .thenReturn(ApiResponseDto.success(fare));

        assertThatThrownBy(() -> service.bookTicket(
                request, "booking-123", 42L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid fare");
    }

    @Test
    void rejectsMissingIdempotencyKeyBeforeExternalCalls() {
        assertThatThrownBy(() -> service.bookTicket(
                request(), null, 42L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Idempotency-Key");
        verifyNoInteractions(routeServiceClient, vehicleServiceClient);
    }

    @Test
    void cancellationReleasesFutureBookedTicket() {
        Ticket ticket = existingTicket(request());
        ticket.setServiceDate(LocalDate.now().plusDays(1));
        ticket.setDepartureTime(LocalTime.NOON);
        when(ticketRepository.findById(1L))
                .thenReturn(Optional.of(ticket));

        var response = service.cancelTicket(1L, 42L, false);

        assertThat(response.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(response.getCancelledAt()).isNotNull();
    }

    @Test
    void rejectsCancellationAfterDeparture() {
        Ticket ticket = existingTicket(request());
        ticket.setServiceDate(LocalDate.now().minusDays(1));
        ticket.setDepartureTime(LocalTime.NOON);
        when(ticketRepository.findById(1L))
                .thenReturn(Optional.of(ticket));

        assertThatThrownBy(() ->
                service.cancelTicket(1L, 42L, false))
                .isInstanceOf(InvalidTicketStateException.class)
                .hasMessageContaining("after departure");
    }

    private void stubDependencies(
            TicketRequestDto request, int capacity, long reserved) {
        when(routeServiceClient.getFare(
                10L, 100L, 200L, 20L, "42", "PASSENGER"))
                .thenReturn(ApiResponseDto.success(fare()));
        when(vehicleServiceClient.getAvailability(
                10L, 20L, request.getServiceDate(), "42", "PASSENGER"))
                .thenReturn(ApiResponseDto.success(
                        availability(request, capacity)));
        when(ticketRepository.countReservedPassengers(
                eq(10L), eq(20L), eq(request.getServiceDate()),
                anyCollection(), eq(TicketStatus.PENDING_PAYMENT),
                any(LocalDateTime.class))).thenReturn(reserved);
    }

    private TicketRequestDto request() {
        TicketRequestDto request = new TicketRequestDto();
        request.setRouteId(10L);
        request.setScheduleId(20L);
        request.setSourceStopId(100L);
        request.setDestinationStopId(200L);
        request.setServiceDate(LocalDate.now().plusDays(1));
        request.setPassengerCount(2);
        return request;
    }

    private FareResponseDto fare() {
        return FareResponseDto.builder()
                .routeId(10L)
                .scheduleId(20L)
                .sourceStopId(100L)
                .destinationStopId(200L)
                .departureTime(LocalTime.NOON)
                .distanceKm(new BigDecimal("10.00"))
                .fare(new BigDecimal("25.00"))
                .build();
    }

    private VehicleAvailabilityResponseDto availability(
            TicketRequestDto request, int capacity) {
        VehicleAvailabilityResponseDto availability =
                new VehicleAvailabilityResponseDto();
        availability.setAssigned(true);
        availability.setAssignmentId(7L);
        availability.setVehicleId(8L);
        availability.setRouteId(10L);
        availability.setScheduleId(20L);
        availability.setServiceDate(request.getServiceDate());
        availability.setCapacity(capacity);
        return availability;
    }

    private Ticket existingTicket(TicketRequestDto request) {
        return Ticket.builder()
                .ticketId(1L)
                .pnrNumber("PNR0000001")
                .userId(42L)
                .routeId(request.getRouteId())
                .scheduleId(request.getScheduleId())
                .sourceStopId(request.getSourceStopId())
                .destinationStopId(request.getDestinationStopId())
                .serviceDate(request.getServiceDate())
                .departureTime(LocalTime.NOON)
                .passengerCount(request.getPassengerCount())
                .unitFare(new BigDecimal("25.00"))
                .fareAmount(new BigDecimal("50.00"))
                .idempotencyKey("booking-123")
                .status(TicketStatus.BOOKED)
                .bookedAt(LocalDateTime.now())
                .build();
    }
}
