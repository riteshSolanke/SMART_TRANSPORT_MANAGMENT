package com.transport.vehicleservice.service;

import com.transport.vehicleservice.dto.request.VehicleRequestDto;
import com.transport.vehicleservice.entity.Vehicle;
import com.transport.vehicleservice.enums.AssignmentStatus;
import com.transport.vehicleservice.enums.VehicleStatus;
import com.transport.vehicleservice.exception.AssignmentConflictException;
import com.transport.vehicleservice.exception.DuplicateVehicleException;
import com.transport.vehicleservice.repository.VehicleAssignmentRepository;
import com.transport.vehicleservice.repository.VehicleRepository;
import com.transport.vehicleservice.service.impl.VehicleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VehicleServiceImplTest {
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private VehicleAssignmentRepository assignmentRepository;
    private VehicleServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new VehicleServiceImpl(vehicleRepository, assignmentRepository);
        when(vehicleRepository.save(any(Vehicle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsAvailableVehicleWithNormalizedNumber() {
        VehicleRequestDto request = request("  bus-101 ", 40);

        var response = service.create(request);

        assertThat(response.getVehicleNumber()).isEqualTo("BUS-101");
        assertThat(response.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    void rejectsDuplicateVehicleNumber() {
        when(vehicleRepository.existsByVehicleNumberIgnoreCase("BUS-101"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(request("bus-101", 40)))
                .isInstanceOf(DuplicateVehicleException.class);
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void rejectsMaintenanceWhileAssignmentIsActive() {
        Vehicle vehicle = vehicle(1L, VehicleStatus.AVAILABLE);
        when(vehicleRepository.findByVehicleIdAndActiveTrue(1L))
                .thenReturn(Optional.of(vehicle));
        when(assignmentRepository.existsByVehicle_VehicleIdAndStatus(
                1L, AssignmentStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() ->
                service.updateStatus(1L, VehicleStatus.MAINTENANCE))
                .isInstanceOf(AssignmentConflictException.class);
    }

    @Test
    void softDeletesVehicleWithoutActiveAssignment() {
        Vehicle vehicle = vehicle(1L, VehicleStatus.AVAILABLE);
        when(vehicleRepository.findByVehicleIdAndActiveTrue(1L))
                .thenReturn(Optional.of(vehicle));

        service.delete(1L);

        assertThat(vehicle.isActive()).isFalse();
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void rejectsDuplicateNumberDuringUpdate() {
        when(vehicleRepository.findByVehicleIdAndActiveTrue(1L))
                .thenReturn(Optional.of(vehicle(1L, VehicleStatus.AVAILABLE)));
        when(vehicleRepository
                .existsByVehicleNumberIgnoreCaseAndVehicleIdNot(
                        "BUS-200", 1L)).thenReturn(true);

        assertThatThrownBy(() ->
                service.update(1L, request("bus-200", 50)))
                .isInstanceOf(DuplicateVehicleException.class);
    }

    private VehicleRequestDto request(String number, int capacity) {
        VehicleRequestDto request = new VehicleRequestDto();
        request.setVehicleNumber(number);
        request.setCapacity(capacity);
        return request;
    }

    private Vehicle vehicle(Long id, VehicleStatus status) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(id);
        vehicle.setVehicleNumber("BUS-101");
        vehicle.setCapacity(40);
        vehicle.setStatus(status);
        vehicle.setActive(true);
        return vehicle;
    }
}
