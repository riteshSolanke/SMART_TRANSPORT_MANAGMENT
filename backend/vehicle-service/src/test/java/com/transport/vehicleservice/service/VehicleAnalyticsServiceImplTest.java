package com.transport.vehicleservice.service;

import com.transport.vehicleservice.enums.AssignmentStatus;
import com.transport.vehicleservice.repository.VehicleAssignmentRepository;
import com.transport.vehicleservice.repository.projection.AssignmentStatusAnalyticsProjection;
import com.transport.vehicleservice.repository.projection.CompletedAssignmentAnalyticsProjection;
import com.transport.vehicleservice.service.impl.VehicleAnalyticsServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VehicleAnalyticsServiceImplTest {
    @Test
    void summarizesAssignmentOutcomesAndCompletedOperations() {
        VehicleAssignmentRepository repository =
                mock(VehicleAssignmentRepository.class);
        List<AssignmentStatusAnalyticsProjection> statusRows = List.of(
                status(AssignmentStatus.ACTIVE, 1),
                status(AssignmentStatus.COMPLETED, 2),
                status(AssignmentStatus.CANCELLED, 1));
        List<CompletedAssignmentAnalyticsProjection> completedRows =
                List.of(completed());
        when(repository.summarizeByStatus(FROM, TO))
                .thenReturn(statusRows);
        when(repository.findCompletedForAnalytics(
                FROM, TO, AssignmentStatus.COMPLETED))
                .thenReturn(completedRows);

        var response =
                new VehicleAnalyticsServiceImpl(repository)
                        .summarize(FROM, TO);

        assertThat(response.getTotalAssignments()).isEqualTo(4);
        assertThat(response.getActiveAssignments()).isEqualTo(1);
        assertThat(response.getCompletedAssignments()).isEqualTo(2);
        assertThat(response.getCancelledAssignments()).isEqualTo(1);
        assertThat(response.getCompletedOperations()).hasSize(1);
    }

    private AssignmentStatusAnalyticsProjection status(
            AssignmentStatus status, long count) {
        AssignmentStatusAnalyticsProjection row =
                mock(AssignmentStatusAnalyticsProjection.class);
        when(row.getStatus()).thenReturn(status);
        when(row.getAssignmentCount()).thenReturn(count);
        return row;
    }

    private CompletedAssignmentAnalyticsProjection completed() {
        CompletedAssignmentAnalyticsProjection row =
                mock(CompletedAssignmentAnalyticsProjection.class);
        when(row.getAssignmentId()).thenReturn(1L);
        when(row.getRouteId()).thenReturn(10L);
        when(row.getScheduleId()).thenReturn(20L);
        when(row.getServiceDate()).thenReturn(FROM);
        when(row.getCompletedAt())
                .thenReturn(LocalDateTime.of(2026, 7, 1, 12, 0));
        return row;
    }

    private static final LocalDate FROM =
            LocalDate.of(2026, 7, 1);
    private static final LocalDate TO =
            LocalDate.of(2026, 7, 31);
}
