package com.transport.vehicleservice.repository;

import com.transport.vehicleservice.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findAllByActiveTrueOrderByVehicleNumberAsc();
    Optional<Vehicle> findByVehicleIdAndActiveTrue(Long vehicleId);
    boolean existsByVehicleNumberIgnoreCase(String vehicleNumber);
    boolean existsByVehicleNumberIgnoreCaseAndVehicleIdNot(
            String vehicleNumber, Long vehicleId);
}
