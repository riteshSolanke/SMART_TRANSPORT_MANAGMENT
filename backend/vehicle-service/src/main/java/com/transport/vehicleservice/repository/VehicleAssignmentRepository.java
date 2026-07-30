package com.transport.vehicleservice.repository;

import com.transport.vehicleservice.entity.VehicleAssignment;
import com.transport.vehicleservice.enums.AssignmentStatus;
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
}
