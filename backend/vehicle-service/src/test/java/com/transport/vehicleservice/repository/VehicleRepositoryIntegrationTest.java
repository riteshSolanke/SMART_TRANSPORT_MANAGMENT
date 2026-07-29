package com.transport.vehicleservice.repository;

import com.transport.vehicleservice.entity.Vehicle;
import com.transport.vehicleservice.entity.VehicleAssignment;
import com.transport.vehicleservice.entity.VehicleLocation;
import com.transport.vehicleservice.enums.AssignmentStatus;
import com.transport.vehicleservice.enums.VehicleStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "eureka.client.enabled=false"
})
class VehicleRepositoryIntegrationTest {
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private VehicleRepository vehicleRepository;
    @Autowired
    private VehicleAssignmentRepository assignmentRepository;
    @Autowired
    private VehicleLocationRepository locationRepository;

    @Test
    void persistsFleetAssignmentsAndOrderedLocations() {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleNumber("BUS-INT-1");
        vehicle.setCapacity(40);
        vehicle.setStatus(VehicleStatus.IN_SERVICE);
        vehicle.setActive(true);
        entityManager.persist(vehicle);

        LocalDate serviceDate = LocalDate.now().plusDays(1);
        VehicleAssignment assignment = new VehicleAssignment();
        assignment.setVehicle(vehicle);
        assignment.setRouteId(10L);
        assignment.setScheduleId(20L);
        assignment.setServiceDate(serviceDate);
        assignment.setStatus(AssignmentStatus.ACTIVE);
        entityManager.persist(assignment);

        persistLocation(vehicle, "18.520400", "73.856700",
                LocalDateTime.now().minusMinutes(1));
        persistLocation(vehicle, "18.530400", "73.866700",
                LocalDateTime.now());
        entityManager.flush();
        entityManager.clear();

        assertThat(vehicleRepository
                .findAllByActiveTrueOrderByVehicleNumberAsc()).hasSize(1);
        assertThat(assignmentRepository
                .existsByVehicle_VehicleIdAndServiceDateAndStatus(
                        vehicle.getVehicleId(), serviceDate, AssignmentStatus.ACTIVE))
                .isTrue();
        assertThat(locationRepository
                .findFirstByVehicle_VehicleIdOrderByRecordedAtDesc(
                        vehicle.getVehicleId()))
                .get()
                .extracting(VehicleLocation::getLatitude)
                .isEqualTo(new BigDecimal("18.530400"));
    }

    private void persistLocation(
            Vehicle vehicle, String latitude, String longitude,
            LocalDateTime recordedAt) {
        VehicleLocation location = new VehicleLocation();
        location.setVehicle(vehicle);
        location.setLatitude(new BigDecimal(latitude));
        location.setLongitude(new BigDecimal(longitude));
        location.setSpeedKph(new BigDecimal("35.50"));
        location.setRecordedAt(recordedAt);
        entityManager.persist(location);
    }
}
