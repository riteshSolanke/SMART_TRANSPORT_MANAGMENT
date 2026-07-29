package com.transport.ticketservice.service.impl;

import com.transport.ticketservice.client.RouteServiceClient;
import com.transport.ticketservice.dto.request.TicketRequestDto;
import com.transport.ticketservice.dto.response.ApiResponseDto;
import com.transport.ticketservice.dto.response.FareResponseDto;
import com.transport.ticketservice.dto.response.TicketResponseDto;
import com.transport.ticketservice.entity.Ticket;
import com.transport.ticketservice.enums.TicketStatus;
import com.transport.ticketservice.exception.InvalidTicketStateException;
import com.transport.ticketservice.exception.TicketNotFoundException;
import com.transport.ticketservice.mapper.TicketMapper;
import com.transport.ticketservice.repository.TicketRepository;
import com.transport.ticketservice.service.TicketService;
import com.transport.ticketservice.util.TicketNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {
    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final RouteServiceClient routeServiceClient;

    private final TicketNumberGenerator ticketNumberGenerator;

    @Override
    @Transactional
    public TicketResponseDto bookTicket(
            TicketRequestDto dto, Long authenticatedUserId, boolean privileged) {
        Long ticketOwnerId = resolveTicketOwner(
                dto.getUserId(), authenticatedUserId, privileged);
        ApiResponseDto<FareResponseDto> response =
                routeServiceClient.getFare(
                        dto.getRouteId(),
                        dto.getSourceStopId(),
                        dto.getDestinationStopId()
                );

        FareResponseDto fareResponse = response.getData();

        String pnr = generateUniquePnr();
        Ticket ticket = Ticket.builder()
                .pnrNumber(pnr)
                .userId(ticketOwnerId)
                .routeId(dto.getRouteId())
                .sourceStopId(dto.getSourceStopId())
                .destinationStopId(dto.getDestinationStopId())
                .fareAmount(fareResponse.getFare())
                .status(TicketStatus.BOOKED)
                .travelDate(dto.getTravelDate())
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);

        return ticketMapper.toResponseDto(savedTicket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponseDto getTicketById(
            Long id, Long authenticatedUserId, boolean privileged) {

       Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));
        verifyTicketAccess(ticket, authenticatedUserId, privileged);

        return ticketMapper.toResponseDto(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponseDto getTicketByPnr(
            String pnrNumber, Long authenticatedUserId, boolean privileged) {
        Ticket ticket = ticketRepository.findByPnrNumber(pnrNumber)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with PNR: " + pnrNumber));
        verifyTicketAccess(ticket, authenticatedUserId, privileged);

        return ticketMapper.toResponseDto(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponseDto> getTicketsByUser(
            Long userId, Long authenticatedUserId, boolean privileged) {
        if (!privileged && !userId.equals(authenticatedUserId)) {
            throw new AccessDeniedException("You can only access your own tickets");
        }
        List<Ticket> tickets = ticketRepository.findByUserId(userId);
        return ticketMapper.toResponseDtoList(tickets);
    }

    @Override
    @Transactional
    public void cancelTicket(Long id, Long authenticatedUserId, boolean privileged) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));
        verifyTicketAccess(ticket, authenticatedUserId, privileged);
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new InvalidTicketStateException("Ticket is already cancelled");
        }

        if (ticket.getStatus() == TicketStatus.USED) {
            throw new InvalidTicketStateException("Cannot cancel a ticket that has already been used");
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setCancelledAt(LocalDateTime.now());
        ticketRepository.save(ticket);
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
            throw new AccessDeniedException("You can only access your own tickets");
        }
    }

}
