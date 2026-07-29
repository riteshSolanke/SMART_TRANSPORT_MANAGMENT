package com.transport.ticketservice.service;

import com.transport.ticketservice.dto.request.TicketRequestDto;

import com.transport.ticketservice.dto.response.TicketResponseDto;

import java.util.List;

public interface TicketService {

    TicketResponseDto bookTicket(
            TicketRequestDto dto, Long authenticatedUserId, boolean privileged);

    TicketResponseDto getTicketById(
            Long id, Long authenticatedUserId, boolean privileged);

    TicketResponseDto getTicketByPnr(
            String pnrNumber, Long authenticatedUserId, boolean privileged);

    List<TicketResponseDto> getTicketsByUser(
            Long userId, Long authenticatedUserId, boolean privileged);

    void cancelTicket(Long id, Long authenticatedUserId, boolean privileged);

}
