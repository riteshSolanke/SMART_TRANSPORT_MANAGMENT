package com.transport.analyticsservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_reports")
@Getter
@Setter
@NoArgsConstructor
public class ServiceReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @Column(name = "generated_by", nullable = false)
    private Long generatedBy;

    @Column(name = "total_ticket_records", nullable = false)
    private Long totalTicketRecords;

    @Column(name = "confirmed_tickets", nullable = false)
    private Long confirmedTickets;

    @Column(name = "total_passengers", nullable = false)
    private Long totalPassengers;

    @Column(name = "cancelled_tickets", nullable = false)
    private Long cancelledTickets;

    @Column(name = "total_payment_attempts", nullable = false)
    private Long totalPaymentAttempts;

    @Column(name = "successful_payments", nullable = false)
    private Long successfulPayments;

    @Column(name = "failed_payments", nullable = false)
    private Long failedPayments;

    @Column(name = "refunded_payments", nullable = false)
    private Long refundedPayments;

    @Column(name = "gross_revenue", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossRevenue;

    @Column(name = "refunded_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundedAmount;

    @Column(name = "total_revenue", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRevenue;

    @Column(name = "total_assignments", nullable = false)
    private Long totalAssignments;

    @Column(name = "completed_assignments", nullable = false)
    private Long completedAssignments;

    @Column(name = "cancelled_assignments", nullable = false)
    private Long cancelledAssignments;

    @Column(name = "on_time_assignments", nullable = false)
    private Long onTimeAssignments;

    @Column(
            name = "on_time_performance",
            nullable = false,
            precision = 5,
            scale = 2)
    private BigDecimal onTimePerformance;

    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        generatedAt = LocalDateTime.now();
    }
}
