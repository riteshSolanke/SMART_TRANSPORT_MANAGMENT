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
    public TicketResponseDto bookTicket(TicketRequestDto dto) {
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
                .userId(dto.getUserId())
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
    public TicketResponseDto getTicketById(Long id) {

       Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));

        return ticketMapper.toResponseDto(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponseDto getTicketByPnr(String pnrNumber) {
        Ticket ticket = ticketRepository.findByPnrNumber(pnrNumber)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with PNR: " + pnrNumber));

        return ticketMapper.toResponseDto(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponseDto> getTicketsByUser(Long userId) {
        List<Ticket> tickets = ticketRepository.findByUserId(userId);
        return ticketMapper.toResponseDtoList(tickets);
    }

    @Override
    @Transactional
    public void cancelTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));
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

}
