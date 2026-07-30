package com.transport.analyticsservice.service;

import com.transport.analyticsservice.dto.PaymentAnalyticsDto;
import com.transport.analyticsservice.dto.PerformanceAnalyticsDto;
import com.transport.analyticsservice.dto.ServiceReportResponseDto;
import com.transport.analyticsservice.dto.TicketAnalyticsDto;

import java.time.LocalDate;
import java.util.List;

public interface AnalyticsService {
    TicketAnalyticsDto usage(
            LocalDate from, LocalDate to, Long userId, String role);

    PaymentAnalyticsDto revenue(
            LocalDate from, LocalDate to, Long userId, String role);

    PerformanceAnalyticsDto performance(
            LocalDate from, LocalDate to, Long userId, String role);

    ServiceReportResponseDto generateReport(
            LocalDate from, LocalDate to, Long userId, String role);

    ServiceReportResponseDto getReport(Long reportId);

    List<ServiceReportResponseDto> getReports();
}
