package com.transport.analyticsservice.service.impl;

import com.transport.analyticsservice.client.PaymentAnalyticsClient;
import com.transport.analyticsservice.client.RouteScheduleClient;
import com.transport.analyticsservice.client.TicketAnalyticsClient;
import com.transport.analyticsservice.client.VehicleAnalyticsClient;
import com.transport.analyticsservice.dto.ApiResponseDto;
import com.transport.analyticsservice.dto.CompletedAssignmentAnalyticsDto;
import com.transport.analyticsservice.dto.PaymentAnalyticsDto;
import com.transport.analyticsservice.dto.PerformanceAnalyticsDto;
import com.transport.analyticsservice.dto.ScheduleAnalyticsDto;
import com.transport.analyticsservice.dto.ServiceReportResponseDto;
import com.transport.analyticsservice.dto.TicketAnalyticsDto;
import com.transport.analyticsservice.dto.VehicleAnalyticsDto;
import com.transport.analyticsservice.entity.ServiceReport;
import com.transport.analyticsservice.exception.AnalyticsSourceUnavailableException;
import com.transport.analyticsservice.exception.ReportNotFoundException;
import com.transport.analyticsservice.mapper.ServiceReportMapper;
import com.transport.analyticsservice.repository.ServiceReportRepository;
import com.transport.analyticsservice.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {
    private final TicketAnalyticsClient ticketClient;
    private final PaymentAnalyticsClient paymentClient;
    private final VehicleAnalyticsClient vehicleClient;
    private final RouteScheduleClient routeClient;
    private final ServiceReportRepository reportRepository;
    private final ServiceReportMapper reportMapper;

    @Value("${analytics.on-time-grace-minutes:15}")
    private int onTimeGraceMinutes;

    @Override
    public TicketAnalyticsDto usage(
            LocalDate from, LocalDate to, Long userId, String role) {
        validateRange(from, to);
        try {
            return requireData(
                    ticketClient.summarize(
                            from, to, identity(userId), role),
                    "ticket-service");
        } catch (AnalyticsSourceUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Ticket analytics source call failed", exception);
            throw unavailable("ticket-service");
        }
    }

    @Override
    public PaymentAnalyticsDto revenue(
            LocalDate from, LocalDate to, Long userId, String role) {
        validateRange(from, to);
        try {
            return requireData(
                    paymentClient.summarize(
                            from, to, identity(userId), role),
                    "payment-service");
        } catch (AnalyticsSourceUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Payment analytics source call failed", exception);
            throw unavailable("payment-service");
        }
    }

    @Override
    public PerformanceAnalyticsDto performance(
            LocalDate from, LocalDate to, Long userId, String role) {
        validateRange(from, to);
        VehicleAnalyticsDto vehicles;
        try {
            vehicles = requireData(
                    vehicleClient.summarize(
                            from, to, identity(userId), role),
                    "vehicle-service");
        } catch (AnalyticsSourceUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Vehicle analytics source call failed", exception);
            throw unavailable("vehicle-service");
        }

        Map<ScheduleKey, ScheduleAnalyticsDto> schedules = new HashMap<>();
        long onTime = 0;
        List<CompletedAssignmentAnalyticsDto> operations =
                vehicles.getCompletedOperations() == null
                        ? List.of() : vehicles.getCompletedOperations();
        for (CompletedAssignmentAnalyticsDto operation : operations) {
            ScheduleKey key = new ScheduleKey(
                    operation.getRouteId(), operation.getScheduleId());
            ScheduleAnalyticsDto schedule = schedules.computeIfAbsent(
                    key,
                    ignored -> getSchedule(
                            operation.getRouteId(),
                            operation.getScheduleId(),
                            userId,
                            role));
            if (isOnTime(operation, schedule)) {
                onTime++;
            }
        }
        long completed = vehicles.getCompletedAssignments();
        BigDecimal percentage = completed == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(onTime * 100)
                        .divide(
                                BigDecimal.valueOf(completed),
                                2,
                                RoundingMode.HALF_UP);
        return PerformanceAnalyticsDto.builder()
                .from(from)
                .to(to)
                .totalAssignments(vehicles.getTotalAssignments())
                .activeAssignments(vehicles.getActiveAssignments())
                .completedAssignments(completed)
                .cancelledAssignments(vehicles.getCancelledAssignments())
                .onTimeAssignments(onTime)
                .onTimePerformance(percentage)
                .graceMinutes(onTimeGraceMinutes)
                .build();
    }

    @Override
    @Transactional
    public ServiceReportResponseDto generateReport(
            LocalDate from,
            LocalDate to,
            Long userId,
            String role) {
        TicketAnalyticsDto usage = usage(from, to, userId, role);
        PaymentAnalyticsDto revenue = revenue(from, to, userId, role);
        PerformanceAnalyticsDto performance =
                performance(from, to, userId, role);

        ServiceReport report = new ServiceReport();
        report.setPeriodStart(from);
        report.setPeriodEnd(to);
        report.setGeneratedBy(userId);
        report.setTotalTicketRecords(usage.getTotalTicketRecords());
        report.setConfirmedTickets(usage.getConfirmedTickets());
        report.setTotalPassengers(usage.getTotalPassengers());
        report.setCancelledTickets(usage.getCancelledTickets());
        report.setTotalPaymentAttempts(revenue.getTotalAttempts());
        report.setSuccessfulPayments(revenue.getSuccessfulPayments());
        report.setFailedPayments(revenue.getFailedPayments());
        report.setRefundedPayments(revenue.getRefundedPayments());
        report.setGrossRevenue(money(revenue.getGrossRevenue()));
        report.setRefundedAmount(money(revenue.getRefundedAmount()));
        report.setTotalRevenue(money(revenue.getNetRevenue()));
        report.setTotalAssignments(performance.getTotalAssignments());
        report.setCompletedAssignments(performance.getCompletedAssignments());
        report.setCancelledAssignments(performance.getCancelledAssignments());
        report.setOnTimeAssignments(performance.getOnTimeAssignments());
        report.setOnTimePerformance(performance.getOnTimePerformance());
        return reportMapper.toResponse(reportRepository.save(report));
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceReportResponseDto getReport(Long reportId) {
        ServiceReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(
                        "Service report not found with id: " + reportId));
        return reportMapper.toResponse(report);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceReportResponseDto> getReports() {
        return reportMapper.toResponse(
                reportRepository.findAllByOrderByGeneratedAtDesc());
    }

    private ScheduleAnalyticsDto getSchedule(
            Long routeId, Long scheduleId, Long userId, String role) {
        try {
            ScheduleAnalyticsDto schedule = requireData(
                    routeClient.getSchedule(
                            routeId, scheduleId, identity(userId), role),
                    "route-service");
            if (!scheduleId.equals(schedule.getScheduleId())
                    || schedule.getDepartureTime() == null
                    || schedule.getArrivalTime() == null) {
                throw unavailable("route-service");
            }
            return schedule;
        } catch (AnalyticsSourceUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn(
                    "Route schedule analytics source call failed for route {} schedule {}",
                    routeId,
                    scheduleId,
                    exception);
            throw unavailable("route-service");
        }
    }

    private boolean isOnTime(
            CompletedAssignmentAnalyticsDto operation,
            ScheduleAnalyticsDto schedule) {
        if (operation.getServiceDate() == null
                || operation.getCompletedAt() == null) {
            return false;
        }
        LocalTime departure = schedule.getDepartureTime();
        LocalTime arrival = schedule.getArrivalTime();
        LocalDateTime scheduledArrival =
                LocalDateTime.of(operation.getServiceDate(), arrival);
        if (!arrival.isAfter(departure)) {
            scheduledArrival = scheduledArrival.plusDays(1);
        }
        return !operation.getCompletedAt().isAfter(
                scheduledArrival.plusMinutes(onTimeGraceMinutes));
    }

    private <T> T requireData(
            ApiResponseDto<T> response, String source) {
        if (response == null || !response.isSuccess()
                || response.getData() == null) {
            throw unavailable(source);
        }
        return response.getData();
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "A valid from/to date range is required");
        }
        if (ChronoUnit.DAYS.between(from, to) > 366) {
            throw new IllegalArgumentException(
                    "Analytics date range cannot exceed 366 days");
        }
    }

    private AnalyticsSourceUnavailableException unavailable(String source) {
        return new AnalyticsSourceUnavailableException(
                source + " is temporarily unavailable for analytics");
    }

    private String identity(Long userId) {
        return userId.toString();
    }

    private BigDecimal money(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(2)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private record ScheduleKey(Long routeId, Long scheduleId) {
    }
}
