package com.transport.ticketservice.service.impl;

import com.transport.ticketservice.client.RouteServiceClient;
import com.transport.ticketservice.client.VehicleServiceClient;
import com.transport.ticketservice.dto.request.TicketRequestDto;
import com.transport.ticketservice.dto.response.ApiResponseDto;
import com.transport.ticketservice.dto.response.FareResponseDto;
import com.transport.ticketservice.dto.response.TicketResponseDto;
import com.transport.ticketservice.dto.response.VehicleAvailabilityResponseDto;
import com.transport.ticketservice.entity.Ticket;
import com.transport.ticketservice.enums.TicketStatus;
import com.transport.ticketservice.exception.IdempotencyConflictException;
import com.transport.ticketservice.exception.InvalidTicketStateException;
import com.transport.ticketservice.exception.SeatUnavailableException;
import com.transport.ticketservice.exception.TicketNotFoundException;
import com.transport.ticketservice.mapper.TicketMapper;
import com.transport.ticketservice.repository.TicketRepository;
import com.transport.ticketservice.service.TicketService;
import com.transport.ticketservice.util.BookingRequestHasher;
import com.transport.ticketservice.util.TicketNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {
    private static final Pattern IDEMPOTENCY_KEY_PATTERN =
            Pattern.compile("^[A-Za-z0-9._:-]{8,64}$");
    private static final EnumSet<TicketStatus> CAPACITY_HOLDING_STATUSES =
            EnumSet.of(TicketStatus.BOOKED, TicketStatus.USED);

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final RouteServiceClient routeServiceClient;
    private final VehicleServiceClient vehicleServiceClient;
    private final TicketNumberGenerator ticketNumberGenerator;
    private final BookingRequestHasher requestHasher;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TicketResponseDto bookTicket(
            TicketRequestDto request,
            String rawIdempotencyKey,
            Long authenticatedUserId,
            boolean privileged) {
        Long ownerId = resolveTicketOwner(
                request.getUserId(), authenticatedUserId, privileged);
        int passengerCount = normalizePassengerCount(request.getPassengerCount());
        request.setPassengerCount(passengerCount);
        String idempotencyKey = normalizeIdempotencyKey(rawIdempotencyKey);
        String requestHash = requestHasher.hash(ownerId, request);

        Ticket existing = ticketRepository
                .findByUserIdAndIdempotencyKey(ownerId, idempotencyKey)
                .orElse(null);
        if (existing != null) {
            if (!requestHash.equals(existing.getRequestHash())) {
                throw new IdempotencyConflictException(
                        "Idempotency key was already used for a different booking request");
            }
            return ticketMapper.toResponseDto(existing);
        }

        FareResponseDto fare = requireFare(routeServiceClient.getFare(
                request.getRouteId(),
                request.getSourceStopId(),
                request.getDestinationStopId(),
                request.getScheduleId()), request);

        if (!LocalDateTime.of(request.getServiceDate(), fare.getDepartureTime())
                .isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Selected schedule has already departed");
        }

        VehicleAvailabilityResponseDto availability =
                requireAvailability(vehicleServiceClient.getAvailability(
                        request.getRouteId(),
                        request.getScheduleId(),
                        request.getServiceDate()), request);

        long alreadyReserved = ticketRepository.countReservedPassengers(
                request.getRouteId(),
                request.getScheduleId(),
                request.getServiceDate(),
                CAPACITY_HOLDING_STATUSES);
        if (alreadyReserved + passengerCount > availability.getCapacity()) {
            throw new SeatUnavailableException(
                    "Only " + Math.max(
                            availability.getCapacity() - alreadyReserved, 0)
                            + " seat(s) remain for this schedule");
        }

        BigDecimal unitFare = fare.getFare().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalFare = unitFare
                .multiply(BigDecimal.valueOf(passengerCount))
                .setScale(2, RoundingMode.HALF_UP);

        Ticket ticket = Ticket.builder()
                .pnrNumber(generateUniquePnr())
                .userId(ownerId)
                .routeId(request.getRouteId())
                .scheduleId(request.getScheduleId())
                .sourceStopId(request.getSourceStopId())
                .destinationStopId(request.getDestinationStopId())
                .serviceDate(request.getServiceDate())
                .departureTime(fare.getDepartureTime())
                .passengerCount(passengerCount)
                .unitFare(unitFare)
                .fareAmount(totalFare)
                .assignmentId(availability.getAssignmentId())
                .vehicleId(availability.getVehicleId())
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .status(TicketStatus.BOOKED)
                .travelDate(LocalDateTime.of(
                        request.getServiceDate(), fare.getDepartureTime()))
                .build();
        return ticketMapper.toResponseDto(ticketRepository.save(ticket));
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponseDto getTicketById(
            Long id, Long authenticatedUserId, boolean privileged) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(
                        "Ticket not found with id: " + id));
        verifyTicketAccess(ticket, authenticatedUserId, privileged);
        return ticketMapper.toResponseDto(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponseDto getTicketByPnr(
            String pnrNumber, Long authenticatedUserId, boolean privileged) {
        Ticket ticket = ticketRepository.findByPnrNumber(pnrNumber)
                .orElseThrow(() -> new TicketNotFoundException(
                        "Ticket not found with PNR: " + pnrNumber));
        verifyTicketAccess(ticket, authenticatedUserId, privileged);
        return ticketMapper.toResponseDto(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponseDto> getTicketsByUser(
            Long userId, Long authenticatedUserId, boolean privileged) {
        if (!privileged && !userId.equals(authenticatedUserId)) {
            throw new AccessDeniedException(
                    "You can only access your own tickets");
        }
        return ticketMapper.toResponseDtoList(
                ticketRepository.findByUserIdOrderByBookedAtDesc(userId));
    }

    @Override
    @Transactional
    public TicketResponseDto cancelTicket(
            Long id, Long authenticatedUserId, boolean privileged) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(
                        "Ticket not found with id: " + id));
        verifyTicketAccess(ticket, authenticatedUserId, privileged);
        if (ticket.getStatus() != TicketStatus.BOOKED) {
            throw new InvalidTicketStateException(
                    "Only a booked ticket can be cancelled");
        }
        LocalDateTime departure = ticket.getServiceDate() != null
                && ticket.getDepartureTime() != null
                ? LocalDateTime.of(
                        ticket.getServiceDate(), ticket.getDepartureTime())
                : ticket.getTravelDate();
        if (departure != null && !departure.isAfter(LocalDateTime.now())) {
            throw new InvalidTicketStateException(
                    "Cannot cancel a ticket after departure");
        }
        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setCancelledAt(LocalDateTime.now());
        return ticketMapper.toResponseDto(ticketRepository.save(ticket));
    }

    private FareResponseDto requireFare(
            ApiResponseDto<FareResponseDto> response, TicketRequestDto request) {
        FareResponseDto fare =
                response == null || !response.isSuccess()
                        ? null : response.getData();
        if (fare == null
                || !request.getRouteId().equals(fare.getRouteId())
                || !request.getScheduleId().equals(fare.getScheduleId())
                || !request.getSourceStopId().equals(fare.getSourceStopId())
                || !request.getDestinationStopId().equals(fare.getDestinationStopId())
                || fare.getDepartureTime() == null
                || fare.getFare() == null
                || fare.getFare().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Route service returned an invalid fare quotation");
        }
        return fare;
    }

    private VehicleAvailabilityResponseDto requireAvailability(
            ApiResponseDto<VehicleAvailabilityResponseDto> response,
            TicketRequestDto request) {
        VehicleAvailabilityResponseDto availability =
                response == null || !response.isSuccess()
                        ? null : response.getData();
        if (availability == null
                || !availability.isAssigned()
                || !request.getRouteId().equals(availability.getRouteId())
                || !request.getScheduleId().equals(availability.getScheduleId())
                || !request.getServiceDate().equals(availability.getServiceDate())
                || availability.getAssignmentId() == null
                || availability.getVehicleId() == null
                || availability.getCapacity() == null
                || availability.getCapacity() < 1) {
            throw new SeatUnavailableException(
                    "No active vehicle assignment is available for this schedule");
        }
        return availability;
    }

    private int normalizePassengerCount(Integer passengerCount) {
        int count = passengerCount == null ? 1 : passengerCount;
        if (count < 1 || count > 10) {
            throw new IllegalArgumentException(
                    "Passenger count must be between 1 and 10");
        }
        return count;
    }

    private String normalizeIdempotencyKey(String rawKey) {
        String key = rawKey == null ? "" : rawKey.trim();
        if (!IDEMPOTENCY_KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException(
                    "Idempotency-Key must be 8 to 64 URL-safe characters");
        }
        return key;
    }

    private String generateUniquePnr() {
        String pnr;
        do {
            pnr = ticketNumberGenerator.generate();
        } while (ticketRepository.existsByPnrNumber(pnr));
        return pnr;
    }

    private Long resolveTicketOwner(
            Long requestedUserId, Long authenticatedUserId, boolean privileged) {
        if (requestedUserId == null || requestedUserId.equals(authenticatedUserId)) {
            return authenticatedUserId;
        }
        if (!privileged) {
            throw new AccessDeniedException(
                    "Passengers cannot book tickets for another user");
        }
        return requestedUserId;
    }

    private void verifyTicketAccess(
            Ticket ticket, Long authenticatedUserId, boolean privileged) {
        if (!privileged && !ticket.getUserId().equals(authenticatedUserId)) {
            throw new AccessDeniedException(
                    "You can only access your own tickets");
        }
    }
}
