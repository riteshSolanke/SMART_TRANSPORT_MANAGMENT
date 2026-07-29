package com.transport.ticketservice.mapper;

import com.transport.ticketservice.dto.response.TicketResponseDto;
import com.transport.ticketservice.entity.Ticket;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {

    public TicketResponseDto toResponseDto(Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        return TicketResponseDto.builder()
                .ticketId(ticket.getTicketId())
                .pnrNumber(ticket.getPnrNumber())
                .userId(ticket.getUserId())
                .routeId(ticket.getRouteId())
                .sourceStopId(ticket.getSourceStopId())
                .destinationStopId(ticket.getDestinationStopId())
                .fareAmount(ticket.getFareAmount())
                .status(ticket.getStatus())
                .bookedAt(ticket.getBookedAt())
                .cancelledAt(ticket.getCancelledAt())
                .travelDate(ticket.getTravelDate())
                .build();

    }

    public java.util.List<TicketResponseDto> toResponseDtoList(java.util.List<Ticket> tickets) {

        return tickets.stream()
                .map(this::toResponseDto)
                .toList();
    }

}
