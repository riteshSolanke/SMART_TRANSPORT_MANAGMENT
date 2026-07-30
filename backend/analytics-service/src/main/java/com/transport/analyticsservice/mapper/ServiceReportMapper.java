package com.transport.analyticsservice.mapper;

import com.transport.analyticsservice.dto.ServiceReportResponseDto;
import com.transport.analyticsservice.entity.ServiceReport;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ServiceReportMapper {
    public ServiceReportResponseDto toResponse(ServiceReport report) {
        return ServiceReportResponseDto.builder()
                .reportId(report.getReportId())
                .periodStart(report.getPeriodStart())
                .periodEnd(report.getPeriodEnd())
                .generatedAt(report.getGeneratedAt())
                .generatedBy(report.getGeneratedBy())
                .totalTicketRecords(report.getTotalTicketRecords())
                .confirmedTickets(report.getConfirmedTickets())
                .totalPassengers(report.getTotalPassengers())
                .cancelledTickets(report.getCancelledTickets())
                .totalPaymentAttempts(report.getTotalPaymentAttempts())
                .successfulPayments(report.getSuccessfulPayments())
                .failedPayments(report.getFailedPayments())
                .refundedPayments(report.getRefundedPayments())
                .grossRevenue(report.getGrossRevenue())
                .refundedAmount(report.getRefundedAmount())
                .totalRevenue(report.getTotalRevenue())
                .totalAssignments(report.getTotalAssignments())
                .completedAssignments(report.getCompletedAssignments())
                .cancelledAssignments(report.getCancelledAssignments())
                .onTimeAssignments(report.getOnTimeAssignments())
                .onTimePerformance(report.getOnTimePerformance())
                .build();
    }

    public List<ServiceReportResponseDto> toResponse(
            List<ServiceReport> reports) {
        return reports.stream().map(this::toResponse).toList();
    }
}
