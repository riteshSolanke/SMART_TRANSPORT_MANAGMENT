package com.transport.vehicleservice.repository;

import com.transport.vehicleservice.entity.VehicleAssignment;
import com.transport.vehicleservice.enums.AssignmentStatus;
import com.transport.vehicleservice.repository.projection.AssignmentStatusAnalyticsProjection;
import com.transport.vehicleservice.repository.projection.CompletedAssignmentAnalyticsProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VehicleAssignmentRepository
        extends JpaRepository<VehicleAssignment, Long> {
    boolean existsByVehicle_VehicleIdAndServiceDateAndStatus(
            Long vehicleId, LocalDate serviceDate, AssignmentStatus status);

    boolean existsByRouteIdAndScheduleIdAndServiceDateAndStatus(
            Long routeId, Long scheduleId, LocalDate serviceDate,
            AssignmentStatus status);

    Optional<VehicleAssignment> findByRouteIdAndScheduleIdAndServiceDateAndStatus(
            Long routeId, Long scheduleId, LocalDate serviceDate,
            AssignmentStatus status);

    boolean existsByVehicle_VehicleIdAndStatus(
            Long vehicleId, AssignmentStatus status);

    List<VehicleAssignment> findByVehicle_VehicleIdOrderByServiceDateDescAssignedAtDesc(
            Long vehicleId);

    Optional<VehicleAssignment> findByAssignmentIdAndVehicle_VehicleIdAndStatus(
            Long assignmentId, Long vehicleId, AssignmentStatus status);

    @Query("""
            SELECT a.status AS status,
                   COUNT(a) AS assignmentCount
            FROM VehicleAssignment a
            WHERE a.serviceDate BETWEEN :from AND :to
            GROUP BY a.status
            """)
    List<AssignmentStatusAnalyticsProjection> summarizeByStatus(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            SELECT a.assignmentId AS assignmentId,
                   a.routeId AS routeId,
                   a.scheduleId AS scheduleId,
                   a.serviceDate AS serviceDate,
                   a.completedAt AS completedAt
            FROM VehicleAssignment a
            WHERE a.serviceDate BETWEEN :from AND :to
              AND a.status = :status
              AND a.completedAt IS NOT NULL
            ORDER BY a.serviceDate, a.assignmentId
            """)
    List<CompletedAssignmentAnalyticsProjection> findCompletedForAnalytics(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("status") AssignmentStatus status);
}
