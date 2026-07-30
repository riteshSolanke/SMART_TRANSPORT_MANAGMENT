package com.transport.vehicleservice.service.impl;

import com.transport.vehicleservice.dto.response.CompletedAssignmentAnalyticsDto;
import com.transport.vehicleservice.dto.response.VehicleAnalyticsDto;
import com.transport.vehicleservice.enums.AssignmentStatus;
import com.transport.vehicleservice.repository.VehicleAssignmentRepository;
import com.transport.vehicleservice.repository.projection.AssignmentStatusAnalyticsProjection;
import com.transport.vehicleservice.service.VehicleAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleAnalyticsServiceImpl implements VehicleAnalyticsService {
    private final VehicleAssignmentRepository assignmentRepository;

    @Override
    @Transactional(readOnly = true)
    public VehicleAnalyticsDto summarize(LocalDate from, LocalDate to) {
        validateRange(from, to);
        Map<AssignmentStatus, AssignmentStatusAnalyticsProjection> byStatus =
                assignmentRepository.summarizeByStatus(from, to).stream()
                        .collect(Collectors.toMap(
                                AssignmentStatusAnalyticsProjection::getStatus,
                                Function.identity()));
        long active = count(byStatus.get(AssignmentStatus.ACTIVE));
        long completed = count(byStatus.get(AssignmentStatus.COMPLETED));
        long cancelled = count(byStatus.get(AssignmentStatus.CANCELLED));
        var operations = assignmentRepository.findCompletedForAnalytics(
                        from, to, AssignmentStatus.COMPLETED)
                .stream()
                .map(row -> CompletedAssignmentAnalyticsDto.builder()
                        .assignmentId(row.getAssignmentId())
                        .routeId(row.getRouteId())
                        .scheduleId(row.getScheduleId())
                        .serviceDate(row.getServiceDate())
                        .completedAt(row.getCompletedAt())
                        .build())
                .toList();
        return VehicleAnalyticsDto.builder()
                .from(from)
                .to(to)
                .totalAssignments(active + completed + cancelled)
                .activeAssignments(active)
                .completedAssignments(completed)
                .cancelledAssignments(cancelled)
                .completedOperations(operations)
                .build();
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

    private long count(AssignmentStatusAnalyticsProjection row) {
        return row == null || row.getAssignmentCount() == null
                ? 0 : row.getAssignmentCount();
    }
}
