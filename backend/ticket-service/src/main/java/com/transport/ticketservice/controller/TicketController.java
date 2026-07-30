package com.transport.ticketservice.controller;

import com.transport.ticketservice.dto.request.TicketRequestDto;
import com.transport.ticketservice.dto.response.ApiResponseDto;
import com.transport.ticketservice.dto.response.TicketResponseDto;
import com.transport.ticketservice.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;


    @PostMapping
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDto<TicketResponseDto> bookTicket(
            Authentication authentication,
            @RequestHeader(name = "Idempotency-Key", required = false)
            String idempotencyKey,
            @Valid @RequestBody TicketRequestDto dto) {
        return ApiResponseDto.success(
                "Ticket booked successfully",
                ticketService.bookTicket(
                        dto, idempotencyKey,
                        userId(authentication), isPrivileged(authentication)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN','TRANSPORT_MANAGER')")
    public ApiResponseDto<TicketResponseDto> getTicketById(
            Authentication authentication,
            @PathVariable Long id) {
        return ApiResponseDto.success(ticketService.getTicketById(
                id, userId(authentication), isPrivileged(authentication)));
    }

    @GetMapping("/pnr/{pnrNumber}")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN','TRANSPORT_MANAGER')")
    public ApiResponseDto<TicketResponseDto> getTicketByPnr(
            Authentication authentication,
            @PathVariable String pnrNumber) {
        return ApiResponseDto.success(ticketService.getTicketByPnr(
                pnrNumber, userId(authentication), isPrivileged(authentication)));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN','TRANSPORT_MANAGER')")
    public ApiResponseDto<List<TicketResponseDto>> getTicketsByUser(
            Authentication authentication,
            @PathVariable Long userId) {
        return ApiResponseDto.success(ticketService.getTicketsByUser(
                userId, userId(authentication), isPrivileged(authentication)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public ApiResponseDto<TicketResponseDto> cancelTicket(
            Authentication authentication,
            @PathVariable Long id) {
        return ApiResponseDto.success(
                "Ticket cancelled successfully",
                ticketService.cancelTicket(
                        id, userId(authentication), isPrivileged(authentication)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN','TRANSPORT_MANAGER')")
    public ApiResponseDto<List<TicketResponseDto>> getMyTickets(
            Authentication authentication) {
        Long authenticatedUserId = userId(authentication);
        return ApiResponseDto.success(ticketService.getTicketsByUser(
                authenticatedUserId,
                authenticatedUserId,
                isPrivileged(authentication)));
    }

    private Long userId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }

    private boolean isPrivileged(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .anyMatch(authority -> authority.equals("ROLE_ADMIN")
                        || authority.equals("ROLE_TRANSPORT_MANAGER"));
    }

}
