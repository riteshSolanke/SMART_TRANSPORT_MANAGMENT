package com.transport.ticketservice.service;

import com.transport.ticketservice.dto.request.TicketRequestDto;

import com.transport.ticketservice.dto.response.TicketResponseDto;

import java.util.List;

public interface TicketService {

    TicketResponseDto bookTicket(TicketRequestDto dto);

    TicketResponseDto getTicketById(Long id);

    TicketResponseDto getTicketByPnr(String pnrNumber);

    List<TicketResponseDto> getTicketsByUser(Long userId);

    void cancelTicket(Long id);

}
