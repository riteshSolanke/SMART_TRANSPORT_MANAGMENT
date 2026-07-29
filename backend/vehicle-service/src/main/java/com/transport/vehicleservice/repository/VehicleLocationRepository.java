package com.transport.vehicleservice.repository;

import com.transport.vehicleservice.entity.VehicleLocation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleLocationRepository extends JpaRepository<VehicleLocation, Long> {
    Optional<VehicleLocation> findFirstByVehicle_VehicleIdOrderByRecordedAtDesc(
            Long vehicleId);

    List<VehicleLocation> findByVehicle_VehicleIdOrderByRecordedAtDesc(
            Long vehicleId, Pageable pageable);
}
