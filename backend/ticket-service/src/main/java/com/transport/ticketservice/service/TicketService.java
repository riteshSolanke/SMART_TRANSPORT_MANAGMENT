package com.transport.ticketservice.service;

import com.transport.ticketservice.dto.request.TicketRequestDto;

import com.transport.ticketservice.dto.response.TicketResponseDto;
import com.transport.ticketservice.dto.response.TicketPaymentContextDto;
import com.transport.ticketservice.dto.response.SeatAvailabilityResponseDto;
import com.transport.ticketservice.dto.request.PaymentUpdateRequestDto;

import java.time.LocalDate;
import java.util.List;

public interface TicketService {

    TicketResponseDto bookTicket(
            TicketRequestDto dto, String idempotencyKey,
            Long authenticatedUserId, boolean privileged);

    SeatAvailabilityResponseDto getSeatAvailability(
            Long routeId, Long scheduleId, LocalDate serviceDate,
            Long authenticatedUserId);

    TicketResponseDto getTicketById(
            Long id, Long authenticatedUserId, boolean privileged);

    TicketResponseDto getTicketByPnr(
            String pnrNumber, Long authenticatedUserId, boolean privileged);

    List<TicketResponseDto> getTicketsByUser(
            Long userId, Long authenticatedUserId, boolean privileged);

    TicketResponseDto cancelTicket(
            Long id, Long authenticatedUserId, boolean privileged);

    TicketPaymentContextDto getPaymentContext(
            Long ticketId, Long authenticatedUserId, boolean privileged);

    TicketResponseDto confirmPayment(
            Long ticketId, PaymentUpdateRequestDto request,
            Long authenticatedUserId, boolean privileged);

    TicketResponseDto failPayment(
            Long ticketId, PaymentUpdateRequestDto request,
            Long authenticatedUserId, boolean privileged);

    TicketResponseDto confirmRefund(
            Long ticketId, PaymentUpdateRequestDto request,
            Long authenticatedUserId, boolean privileged);

}
