package com.transport.ticketservice.controller;

import com.transport.ticketservice.dto.request.TicketRequestDto;
import com.transport.ticketservice.dto.response.ApiResponseDto;
import com.transport.ticketservice.dto.response.TicketResponseDto;
import com.transport.ticketservice.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;


    @GetMapping("/test")
    public String test(){
        return "Tested Successfully";
    }


    @PostMapping
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDto<TicketResponseDto> bookTicket(@Valid @RequestBody TicketRequestDto dto) {
        return ApiResponseDto.success("Ticket booked successfully", ticketService.bookTicket(dto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN','TRANSPORT_MANAGER')")
    public ApiResponseDto<TicketResponseDto> getTicketById(@PathVariable Long id) {
        return ApiResponseDto.success(ticketService.getTicketById(id));
    }

    @GetMapping("/pnr/{pnrNumber}")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN','TRANSPORT_MANAGER')")
    public ApiResponseDto<TicketResponseDto> getTicketByPnr(@PathVariable String pnrNumber) {
        return ApiResponseDto.success(ticketService.getTicketByPnr(pnrNumber));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public ApiResponseDto<List<TicketResponseDto>> getTicketsByUser(@PathVariable Long userId) {
        return ApiResponseDto.success(ticketService.getTicketsByUser(userId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public ApiResponseDto<String> cancelTicket(@PathVariable Long id) {
        ticketService.cancelTicket(id);
        return ApiResponseDto.success("Ticket cancelled successfully", "OK");
    }

}
