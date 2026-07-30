package com.transport.analyticsservice.controller;

import com.transport.analyticsservice.dto.ApiResponseDto;
import com.transport.analyticsservice.dto.PaymentAnalyticsDto;
import com.transport.analyticsservice.dto.PerformanceAnalyticsDto;
import com.transport.analyticsservice.dto.ServiceReportResponseDto;
import com.transport.analyticsservice.dto.TicketAnalyticsDto;
import com.transport.analyticsservice.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TRANSPORT_MANAGER','ADMIN')")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    @GetMapping("/usage")
    public ApiResponseDto<TicketAnalyticsDto> usage(
            Authentication authentication,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to) {
        return ApiResponseDto.success(analyticsService.usage(
                from, to, userId(authentication), role(authentication)));
    }

    @GetMapping("/revenue")
    public ApiResponseDto<PaymentAnalyticsDto> revenue(
            Authentication authentication,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to) {
        return ApiResponseDto.success(analyticsService.revenue(
                from, to, userId(authentication), role(authentication)));
    }

    @GetMapping("/performance")
    public ApiResponseDto<PerformanceAnalyticsDto> performance(
            Authentication authentication,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to) {
        return ApiResponseDto.success(analyticsService.performance(
                from, to, userId(authentication), role(authentication)));
    }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDto<ServiceReportResponseDto> generateReport(
            Authentication authentication,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to) {
        return ApiResponseDto.success(
                "Service report generated",
                analyticsService.generateReport(
                        from,
                        to,
                        userId(authentication),
                        role(authentication)));
    }

    @GetMapping("/reports/{reportId}")
    public ApiResponseDto<ServiceReportResponseDto> getReport(
            @PathVariable Long reportId) {
        return ApiResponseDto.success(
                analyticsService.getReport(reportId));
    }

    @GetMapping("/reports")
    public ApiResponseDto<List<ServiceReportResponseDto>> getReports() {
        return ApiResponseDto.success(analyticsService.getReports());
    }

    private Long userId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }

    private String role(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .filter(authority ->
                        "TRANSPORT_MANAGER".equals(authority)
                                || "ADMIN".equals(authority))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "A reporting role is required"));
    }
}
