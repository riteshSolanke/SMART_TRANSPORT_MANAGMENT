package com.transport.analyticsservice.service;

import com.transport.analyticsservice.client.PaymentAnalyticsClient;
import com.transport.analyticsservice.client.RouteScheduleClient;
import com.transport.analyticsservice.client.TicketAnalyticsClient;
import com.transport.analyticsservice.client.VehicleAnalyticsClient;
import com.transport.analyticsservice.dto.ApiResponseDto;
import com.transport.analyticsservice.dto.CompletedAssignmentAnalyticsDto;
import com.transport.analyticsservice.dto.PaymentAnalyticsDto;
import com.transport.analyticsservice.dto.ScheduleAnalyticsDto;
import com.transport.analyticsservice.dto.TicketAnalyticsDto;
import com.transport.analyticsservice.dto.VehicleAnalyticsDto;
import com.transport.analyticsservice.entity.ServiceReport;
import com.transport.analyticsservice.exception.AnalyticsSourceUnavailableException;
import com.transport.analyticsservice.mapper.ServiceReportMapper;
import com.transport.analyticsservice.repository.ServiceReportRepository;
import com.transport.analyticsservice.service.impl.AnalyticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {
    @Mock
    private TicketAnalyticsClient ticketClient;
    @Mock
    private PaymentAnalyticsClient paymentClient;
    @Mock
    private VehicleAnalyticsClient vehicleClient;
    @Mock
    private RouteScheduleClient routeClient;
    @Mock
    private ServiceReportRepository repository;

    private AnalyticsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsServiceImpl(
                ticketClient,
                paymentClient,
                vehicleClient,
                routeClient,
                repository,
                new ServiceReportMapper());
        ReflectionTestUtils.setField(
                service, "onTimeGraceMinutes", 15);
    }

    @Test
    void returnsTicketUsageFromTicketService() {
        TicketAnalyticsDto usage = usage();
        when(ticketClient.summarize(
                FROM, TO, "7", "TRANSPORT_MANAGER"))
                .thenReturn(ApiResponseDto.success(usage));

        var response = service.usage(
                FROM, TO, 7L, "TRANSPORT_MANAGER");

        assertThat(response.getTotalPassengers()).isEqualTo(30);
        assertThat(response.getConfirmedFareValue())
                .isEqualByComparingTo("750.00");
    }

    @Test
    void calculatesOnTimePerformanceAndCachesScheduleLookup() {
        VehicleAnalyticsDto vehicles = vehicles(
                operation(1L, LocalDateTime.of(2026, 7, 10, 11, 10)),
                operation(2L, LocalDateTime.of(2026, 7, 10, 11, 20)));
        when(vehicleClient.summarize(
                FROM, TO, "7", "TRANSPORT_MANAGER"))
                .thenReturn(ApiResponseDto.success(vehicles));
        when(routeClient.getSchedule(
                10L, 20L, "7", "TRANSPORT_MANAGER"))
                .thenReturn(ApiResponseDto.success(schedule(
                        LocalTime.of(10, 0), LocalTime.of(11, 0))));

        var response = service.performance(
                FROM, TO, 7L, "TRANSPORT_MANAGER");

        assertThat(response.getOnTimeAssignments()).isEqualTo(1);
        assertThat(response.getOnTimePerformance())
                .isEqualByComparingTo("50.00");
        verify(routeClient, times(1)).getSchedule(
                10L, 20L, "7", "TRANSPORT_MANAGER");
    }

    @Test
    void handlesScheduleArrivalAfterMidnight() {
        VehicleAnalyticsDto vehicles = vehicles(
                operation(1L, LocalDateTime.of(2026, 7, 11, 1, 5)));
        vehicles.setCompletedAssignments(1);
        vehicles.setTotalAssignments(1);
        when(vehicleClient.summarize(
                FROM, TO, "7", "ADMIN"))
                .thenReturn(ApiResponseDto.success(vehicles));
        when(routeClient.getSchedule(10L, 20L, "7", "ADMIN"))
                .thenReturn(ApiResponseDto.success(schedule(
                        LocalTime.of(23, 0), LocalTime.of(1, 0))));

        var response =
                service.performance(FROM, TO, 7L, "ADMIN");

        assertThat(response.getOnTimeAssignments()).isEqualTo(1);
        assertThat(response.getOnTimePerformance())
                .isEqualByComparingTo("100.00");
    }

    @Test
    void generatedReportPersistsCombinedMetrics() {
        when(ticketClient.summarize(FROM, TO, "7", "ADMIN"))
                .thenReturn(ApiResponseDto.success(usage()));
        when(paymentClient.summarize(FROM, TO, "7", "ADMIN"))
                .thenReturn(ApiResponseDto.success(revenue()));
        when(vehicleClient.summarize(FROM, TO, "7", "ADMIN"))
                .thenReturn(ApiResponseDto.success(
                        vehicles()));
        when(repository.save(any(ServiceReport.class)))
                .thenAnswer(invocation -> {
                    ServiceReport report = invocation.getArgument(0);
                    report.setReportId(99L);
                    return report;
                });

        var report =
                service.generateReport(FROM, TO, 7L, "ADMIN");

        assertThat(report.getReportId()).isEqualTo(99L);
        assertThat(report.getTotalPassengers()).isEqualTo(30L);
        assertThat(report.getGrossRevenue())
                .isEqualByComparingTo("600.00");
        assertThat(report.getRefundedAmount())
                .isEqualByComparingTo("50.00");
        assertThat(report.getTotalRevenue())
                .isEqualByComparingTo("550.00");
    }

    @Test
    void invalidOrUnavailableSourceIsReportedAsServiceUnavailable() {
        when(ticketClient.summarize(
                FROM, TO, "7", "TRANSPORT_MANAGER"))
                .thenReturn(null);

        assertThatThrownBy(() ->
                service.usage(FROM, TO, 7L, "TRANSPORT_MANAGER"))
                .isInstanceOf(AnalyticsSourceUnavailableException.class)
                .hasMessageContaining("ticket-service");
    }

    private TicketAnalyticsDto usage() {
        TicketAnalyticsDto dto = new TicketAnalyticsDto();
        dto.setFrom(FROM);
        dto.setTo(TO);
        dto.setTotalTicketRecords(14);
        dto.setConfirmedTickets(12);
        dto.setTotalPassengers(30);
        dto.setCancelledTickets(2);
        dto.setConfirmedFareValue(new BigDecimal("750.00"));
        dto.setRouteUsage(List.of());
        return dto;
    }

    private PaymentAnalyticsDto revenue() {
        PaymentAnalyticsDto dto = new PaymentAnalyticsDto();
        dto.setFrom(FROM);
        dto.setTo(TO);
        dto.setTotalAttempts(12);
        dto.setSuccessfulPayments(10);
        dto.setFailedPayments(2);
        dto.setRefundedPayments(1);
        dto.setGrossRevenue(new BigDecimal("600.00"));
        dto.setRefundedAmount(new BigDecimal("50.00"));
        dto.setNetRevenue(new BigDecimal("550.00"));
        dto.setPaymentMethods(List.of());
        return dto;
    }

    private VehicleAnalyticsDto vehicles(
            CompletedAssignmentAnalyticsDto... operations) {
        VehicleAnalyticsDto dto = new VehicleAnalyticsDto();
        dto.setFrom(FROM);
        dto.setTo(TO);
        dto.setTotalAssignments(operations.length);
        dto.setCompletedAssignments(operations.length);
        dto.setCompletedOperations(List.of(operations));
        return dto;
    }

    private CompletedAssignmentAnalyticsDto operation(
            Long assignmentId, LocalDateTime completedAt) {
        CompletedAssignmentAnalyticsDto dto =
                new CompletedAssignmentAnalyticsDto();
        dto.setAssignmentId(assignmentId);
        dto.setRouteId(10L);
        dto.setScheduleId(20L);
        dto.setServiceDate(LocalDate.of(2026, 7, 10));
        dto.setCompletedAt(completedAt);
        return dto;
    }

    private ScheduleAnalyticsDto schedule(
            LocalTime departure, LocalTime arrival) {
        ScheduleAnalyticsDto dto = new ScheduleAnalyticsDto();
        dto.setScheduleId(20L);
        dto.setDepartureTime(departure);
        dto.setArrivalTime(arrival);
        dto.setActive(true);
        return dto;
    }

    private static final LocalDate FROM =
            LocalDate.of(2026, 7, 1);
    private static final LocalDate TO =
            LocalDate.of(2026, 7, 31);
}
