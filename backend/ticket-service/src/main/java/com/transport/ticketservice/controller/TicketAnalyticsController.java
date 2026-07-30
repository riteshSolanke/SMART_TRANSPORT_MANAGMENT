package com.transport.ticketservice.controller;

import com.transport.ticketservice.dto.response.ApiResponseDto;
import com.transport.ticketservice.dto.response.TicketAnalyticsDto;
import com.transport.ticketservice.security.AnalyticsServiceKeyValidator;
import com.transport.ticketservice.service.TicketAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/tickets/internal/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TRANSPORT_MANAGER','ADMIN')")
public class TicketAnalyticsController {
    private final TicketAnalyticsService analyticsService;
    private final AnalyticsServiceKeyValidator keyValidator;

    @GetMapping
    public ApiResponseDto<TicketAnalyticsDto> summarize(
            @RequestHeader(
                    name = "X-Analytics-Service-Key", required = false)
            String analyticsKey,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to) {
        keyValidator.validate(analyticsKey);
        return ApiResponseDto.success(analyticsService.summarize(from, to));
    }
}
