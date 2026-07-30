package com.transport.analyticsservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class ServiceReportResponseDto {
    private Long reportId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDateTime generatedAt;
    private Long generatedBy;
    private Long totalTicketRecords;
    private Long confirmedTickets;
    private Long totalPassengers;
    private Long cancelledTickets;
    private Long totalPaymentAttempts;
    private Long successfulPayments;
    private Long failedPayments;
    private Long refundedPayments;
    private BigDecimal grossRevenue;
    private BigDecimal refundedAmount;
    private BigDecimal totalRevenue;
    private Long totalAssignments;
    private Long completedAssignments;
    private Long cancelledAssignments;
    private Long onTimeAssignments;
    private BigDecimal onTimePerformance;
}
