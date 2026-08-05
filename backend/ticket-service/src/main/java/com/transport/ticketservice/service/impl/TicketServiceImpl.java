package com.transport.ticketservice.service.impl;

import com.transport.ticketservice.client.RouteServiceClient;
import com.transport.ticketservice.client.VehicleServiceClient;
import com.transport.ticketservice.dto.request.TicketRequestDto;
import com.transport.ticketservice.dto.request.PaymentUpdateRequestDto;
import com.transport.ticketservice.dto.response.ApiResponseDto;
import com.transport.ticketservice.dto.response.FareResponseDto;
import com.transport.ticketservice.dto.response.TicketResponseDto;
import com.transport.ticketservice.dto.response.TicketPaymentContextDto;
import com.transport.ticketservice.dto.response.SeatAvailabilityResponseDto;
import com.transport.ticketservice.dto.response.VehicleAvailabilityResponseDto;
import com.transport.ticketservice.entity.Ticket;
import com.transport.ticketservice.enums.TicketStatus;
import com.transport.ticketservice.exception.IdempotencyConflictException;
import com.transport.ticketservice.exception.InvalidTicketStateException;
import com.transport.ticketservice.exception.SeatUnavailableException;
import com.transport.ticketservice.exception.TicketNotFoundException;
import com.transport.ticketservice.exception.VehicleServiceUnavailableException;
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
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {
    private static final Pattern IDEMPOTENCY_KEY_PATTERN =
            Pattern.compile("^[A-Za-z0-9._:-]{8,64}$");
    private static final EnumSet<TicketStatus> CONFIRMED_CAPACITY_STATUSES =
            EnumSet.of(TicketStatus.BOOKED, TicketStatus.USED);
    private static final long PAYMENT_HOLD_MINUTES = 10;
    private static final String DOWNSTREAM_ROLE = "PASSENGER";

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
        String downstreamUserId = requireDownstreamUserId(authenticatedUserId);
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
                request.getScheduleId(),
                downstreamUserId,
                DOWNSTREAM_ROLE), request);

        if (!LocalDateTime.of(request.getServiceDate(), fare.getDepartureTime())
                .isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Selected schedule has already departed");
        }

        SeatAvailabilityResponseDto availability = calculateSeatAvailability(
                request.getRouteId(),
                request.getScheduleId(),
                request.getServiceDate(),
                downstreamUserId,
                DOWNSTREAM_ROLE);
        if (!availability.isAssigned()) {
            throw new SeatUnavailableException(
                    "No active vehicle assignment is available for this schedule");
        }
        if (passengerCount > availability.getRemainingSeats()) {
            throw new SeatUnavailableException(
                    "Only " + availability.getRemainingSeats()
                            + " seat(s) remain for this schedule");
        }

        LocalDateTime now = LocalDateTime.now();

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
                .paymentExpiresAt(now.plusMinutes(PAYMENT_HOLD_MINUTES))
                .status(TicketStatus.PENDING_PAYMENT)
                .travelDate(LocalDateTime.of(
                        request.getServiceDate(), fare.getDepartureTime()))
                .build();
        return ticketMapper.toResponseDto(ticketRepository.save(ticket));
    }

    @Override
    @Transactional(readOnly = true)
    public SeatAvailabilityResponseDto getSeatAvailability(
            Long routeId, Long scheduleId, LocalDate serviceDate,
            Long authenticatedUserId) {
        if (routeId == null || scheduleId == null || serviceDate == null) {
            throw new IllegalArgumentException(
                    "Route, schedule and service date are required");
        }
        if (serviceDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Service date cannot be in the past");
        }
        return calculateSeatAvailability(
                routeId, scheduleId, serviceDate,
                requireDownstreamUserId(authenticatedUserId),
                DOWNSTREAM_ROLE);
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
        if (ticket.getStatus() == TicketStatus.BOOKED
                && ticket.getPaymentId() != null) {
            throw new InvalidTicketStateException(
                    "Paid tickets must be cancelled through the refund endpoint");
        }
        if (ticket.getStatus() != TicketStatus.BOOKED
                && ticket.getStatus() != TicketStatus.PENDING_PAYMENT) {
            throw new InvalidTicketStateException(
                    "Only pending or booked tickets can be cancelled");
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
        ticket.setPaymentExpiresAt(null);
        return ticketMapper.toResponseDto(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    public TicketPaymentContextDto getPaymentContext(
            Long ticketId, Long authenticatedUserId, boolean privileged) {
        Ticket ticket = requireAccessibleTicket(
                ticketId, authenticatedUserId, privileged);
        expirePaymentHoldIfNeeded(ticket);
        return toPaymentContext(ticket);
    }

    @Override
    @Transactional
    public TicketResponseDto confirmPayment(
            Long ticketId,
            PaymentUpdateRequestDto request,
            Long authenticatedUserId,
            boolean privileged) {
        Ticket ticket = requireAccessibleTicket(
                ticketId, authenticatedUserId, privileged);
        if (ticket.getStatus() == TicketStatus.BOOKED
                && request.getPaymentId().equals(ticket.getPaymentId())) {
            return ticketMapper.toResponseDto(ticket);
        }
        expirePaymentHoldIfNeeded(ticket);
        if (ticket.getStatus() != TicketStatus.PENDING_PAYMENT) {
            throw new InvalidTicketStateException(
                    "Ticket is not awaiting payment");
        }
        verifyPaymentAmount(ticket, request.getAmount());
        ticket.setStatus(TicketStatus.BOOKED);
        ticket.setPaymentId(request.getPaymentId());
        ticket.setPaidAt(LocalDateTime.now());
        ticket.setPaymentExpiresAt(null);
        return ticketMapper.toResponseDto(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    public TicketResponseDto failPayment(
            Long ticketId,
            PaymentUpdateRequestDto request,
            Long authenticatedUserId,
            boolean privileged) {
        Ticket ticket = requireAccessibleTicket(
                ticketId, authenticatedUserId, privileged);
        if (ticket.getStatus() == TicketStatus.PAYMENT_FAILED
                && request.getPaymentId().equals(ticket.getPaymentId())) {
            return ticketMapper.toResponseDto(ticket);
        }
        if (ticket.getStatus() != TicketStatus.PENDING_PAYMENT) {
            throw new InvalidTicketStateException(
                    "Ticket is not awaiting payment");
        }
        verifyPaymentAmount(ticket, request.getAmount());
        ticket.setStatus(TicketStatus.PAYMENT_FAILED);
        ticket.setPaymentId(request.getPaymentId());
        ticket.setPaymentExpiresAt(null);
        return ticketMapper.toResponseDto(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    public TicketResponseDto confirmRefund(
            Long ticketId,
            PaymentUpdateRequestDto request,
            Long authenticatedUserId,
            boolean privileged) {
        Ticket ticket = requireAccessibleTicket(
                ticketId, authenticatedUserId, privileged);
        if (ticket.getStatus() == TicketStatus.CANCELLED
                && request.getPaymentId().equals(ticket.getPaymentId())) {
            return ticketMapper.toResponseDto(ticket);
        }
        if (ticket.getStatus() != TicketStatus.BOOKED
                || ticket.getPaymentId() == null
                || !ticket.getPaymentId().equals(request.getPaymentId())) {
            throw new InvalidTicketStateException(
                    "Ticket does not have the referenced successful payment");
        }
        verifyPaymentAmount(ticket, request.getAmount());
        ensureBeforeDeparture(ticket);
        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setCancelledAt(LocalDateTime.now());
        return ticketMapper.toResponseDto(ticketRepository.save(ticket));
    }

    private Ticket requireAccessibleTicket(
            Long ticketId, Long authenticatedUserId, boolean privileged) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(
                        "Ticket not found with id: " + ticketId));
        verifyTicketAccess(ticket, authenticatedUserId, privileged);
        return ticket;
    }

    private void expirePaymentHoldIfNeeded(Ticket ticket) {
        if (ticket.getStatus() == TicketStatus.PENDING_PAYMENT
                && ticket.getPaymentExpiresAt() != null
                && !ticket.getPaymentExpiresAt().isAfter(LocalDateTime.now())) {
            ticket.setStatus(TicketStatus.EXPIRED);
            ticket.setPaymentExpiresAt(null);
            ticketRepository.save(ticket);
        }
    }

    private TicketPaymentContextDto toPaymentContext(Ticket ticket) {
        LocalDateTime departure = departureAt(ticket);
        boolean beforeDeparture =
                departure == null || departure.isAfter(LocalDateTime.now());
        return TicketPaymentContextDto.builder()
                .ticketId(ticket.getTicketId())
                .userId(ticket.getUserId())
                .amount(ticket.getFareAmount())
                .status(ticket.getStatus())
                .paymentExpiresAt(ticket.getPaymentExpiresAt())
                .departureAt(departure)
                .paymentId(ticket.getPaymentId())
                .payable(ticket.getStatus() == TicketStatus.PENDING_PAYMENT
                        && ticket.getPaymentExpiresAt() != null
                        && ticket.getPaymentExpiresAt().isAfter(LocalDateTime.now()))
                .refundable(ticket.getStatus() == TicketStatus.BOOKED
                        && ticket.getPaymentId() != null
                        && beforeDeparture)
                .build();
    }

    private void verifyPaymentAmount(Ticket ticket, BigDecimal amount) {
        if (amount == null || ticket.getFareAmount().compareTo(amount) != 0) {
            throw new IllegalArgumentException(
                    "Payment amount does not match ticket total");
        }
    }

    private void ensureBeforeDeparture(Ticket ticket) {
        LocalDateTime departure = departureAt(ticket);
        if (departure != null && !departure.isAfter(LocalDateTime.now())) {
            throw new InvalidTicketStateException(
                    "Ticket departure time has passed");
        }
    }

    private LocalDateTime departureAt(Ticket ticket) {
        return ticket.getServiceDate() != null
                && ticket.getDepartureTime() != null
                ? LocalDateTime.of(
                        ticket.getServiceDate(), ticket.getDepartureTime())
                : ticket.getTravelDate();
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

    private SeatAvailabilityResponseDto calculateSeatAvailability(
            Long routeId, Long scheduleId, LocalDate serviceDate,
            String authenticatedUserId, String authenticatedRole) {
        ApiResponseDto<VehicleAvailabilityResponseDto> response =
                vehicleServiceClient.getAvailability(
                        routeId, scheduleId, serviceDate,
                        authenticatedUserId, authenticatedRole);
        VehicleAvailabilityResponseDto availability =
                response == null || !response.isSuccess()
                        ? null : response.getData();
        if (availability == null
                || !routeId.equals(availability.getRouteId())
                || !scheduleId.equals(availability.getScheduleId())
                || !serviceDate.equals(availability.getServiceDate())) {
            throw new VehicleServiceUnavailableException(
                    "Vehicle Service returned invalid availability data");
        }
        if (!availability.isAssigned()) {
            return SeatAvailabilityResponseDto.builder()
                    .routeId(routeId)
                    .scheduleId(scheduleId)
                    .serviceDate(serviceDate)
                    .assigned(false)
                    .capacity(0)
                    .reservedSeats(0L)
                    .remainingSeats(0L)
                    .available(false)
                    .build();
        }
        if (availability.getAssignmentId() == null
                || availability.getVehicleId() == null
                || availability.getCapacity() == null
                || availability.getCapacity() < 1) {
            throw new VehicleServiceUnavailableException(
                    "Vehicle Service returned incomplete assignment data");
        }
        Long reservedResult = ticketRepository.countReservedPassengers(
                routeId,
                scheduleId,
                serviceDate,
                CONFIRMED_CAPACITY_STATUSES,
                TicketStatus.PENDING_PAYMENT,
                LocalDateTime.now());
        long reservedSeats = reservedResult == null ? 0 : reservedResult;
        long remainingSeats = Math.max(
                (long) availability.getCapacity() - reservedSeats, 0L);
        return SeatAvailabilityResponseDto.builder()
                .routeId(routeId)
                .scheduleId(scheduleId)
                .serviceDate(serviceDate)
                .assigned(true)
                .assignmentId(availability.getAssignmentId())
                .vehicleId(availability.getVehicleId())
                .capacity(availability.getCapacity())
                .reservedSeats(reservedSeats)
                .remainingSeats(remainingSeats)
                .available(remainingSeats > 0)
                .build();
    }

    private int normalizePassengerCount(Integer passengerCount) {
        int count = passengerCount == null ? 1 : passengerCount;
        if (count < 1 || count > 10) {
            throw new IllegalArgumentException(
                    "Passenger count must be between 1 and 10");
        }
        return count;
    }

    private String requireDownstreamUserId(Long authenticatedUserId) {
        if (authenticatedUserId == null) {
            throw new AccessDeniedException("Authenticated user is missing");
        }
        return authenticatedUserId.toString();
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
