package com.transport.vehicleservice.service;

import com.transport.vehicleservice.dto.request.AssignmentRequestDto;
import com.transport.vehicleservice.entity.Vehicle;
import com.transport.vehicleservice.entity.VehicleAssignment;
import com.transport.vehicleservice.enums.AssignmentStatus;
import com.transport.vehicleservice.enums.VehicleStatus;
import com.transport.vehicleservice.exception.AssignmentConflictException;
import com.transport.vehicleservice.repository.VehicleAssignmentRepository;
import com.transport.vehicleservice.repository.VehicleRepository;
import com.transport.vehicleservice.service.impl.AssignmentServiceImpl;
import com.transport.vehicleservice.service.impl.RouteReferenceValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AssignmentServiceImplTest {
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private VehicleAssignmentRepository assignmentRepository;
    @Mock
    private RouteReferenceValidator routeValidator;
    private AssignmentServiceImpl service;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AssignmentServiceImpl(
                vehicleRepository, assignmentRepository, routeValidator);
        vehicle = new Vehicle();
        vehicle.setVehicleId(1L);
        vehicle.setVehicleNumber("BUS-101");
        vehicle.setCapacity(40);
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle.setActive(true);
        when(vehicleRepository.findByVehicleIdAndActiveTrue(1L))
                .thenReturn(Optional.of(vehicle));
        when(assignmentRepository.save(any(VehicleAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsValidatedDatedAssignment() {
        AssignmentRequestDto request = request();

        var response = service.assign(1L, request);

        assertThat(response.getRouteId()).isEqualTo(10L);
        assertThat(response.getScheduleId()).isEqualTo(20L);
        assertThat(response.getStatus()).isEqualTo(AssignmentStatus.ACTIVE);
        verify(routeValidator).validate(10L, 20L, request.getServiceDate());
    }

    @Test
    void rejectsVehicleInMaintenance() {
        vehicle.setStatus(VehicleStatus.MAINTENANCE);

        assertThatThrownBy(() -> service.assign(1L, request()))
                .isInstanceOf(AssignmentConflictException.class);
        verifyNoInteractions(routeValidator);
    }

    @Test
    void rejectsVehicleConflictOnServiceDate() {
        AssignmentRequestDto request = request();
        when(assignmentRepository.existsByVehicle_VehicleIdAndServiceDateAndStatus(
                1L, request.getServiceDate(), AssignmentStatus.ACTIVE))
                .thenReturn(true);

        assertThatThrownBy(() -> service.assign(1L, request))
                .isInstanceOf(AssignmentConflictException.class);
        verifyNoInteractions(routeValidator);
    }

    @Test
    void rejectsScheduleConflictOnServiceDate() {
        AssignmentRequestDto request = request();
        when(assignmentRepository
                .existsByRouteIdAndScheduleIdAndServiceDateAndStatus(
                        10L, 20L, request.getServiceDate(), AssignmentStatus.ACTIVE))
                .thenReturn(true);

        assertThatThrownBy(() -> service.assign(1L, request))
                .isInstanceOf(AssignmentConflictException.class);
        verifyNoInteractions(routeValidator);
    }

    @Test
    void completesOnlyScopedActiveAssignment() {
        VehicleAssignment assignment = new VehicleAssignment();
        assignment.setAssignmentId(7L);
        assignment.setVehicle(vehicle);
        assignment.setRouteId(10L);
        assignment.setScheduleId(20L);
        assignment.setServiceDate(LocalDate.now());
        assignment.setStatus(AssignmentStatus.ACTIVE);
        when(assignmentRepository
                .findByAssignmentIdAndVehicle_VehicleIdAndStatus(
                        7L, 1L, AssignmentStatus.ACTIVE))
                .thenReturn(Optional.of(assignment));

        var response = service.complete(1L, 7L);

        assertThat(response.getStatus()).isEqualTo(AssignmentStatus.COMPLETED);
        assertThat(response.getCompletedAt()).isNotNull();
    }

    @Test
    void returnsCapacityForActiveAssignment() {
        AssignmentRequestDto request = request();
        VehicleAssignment assignment = new VehicleAssignment();
        assignment.setAssignmentId(7L);
        assignment.setVehicle(vehicle);
        assignment.setRouteId(10L);
        assignment.setScheduleId(20L);
        assignment.setServiceDate(request.getServiceDate());
        assignment.setStatus(AssignmentStatus.ACTIVE);
        when(assignmentRepository
                .findByRouteIdAndScheduleIdAndServiceDateAndStatus(
                        10L, 20L, request.getServiceDate(), AssignmentStatus.ACTIVE))
                .thenReturn(Optional.of(assignment));

        var availability = service.getAvailability(
                10L, 20L, request.getServiceDate());

        assertThat(availability.isAssigned()).isTrue();
        assertThat(availability.getCapacity()).isEqualTo(40);
        assertThat(availability.getVehicleId()).isEqualTo(1L);
    }

    @Test
    void reportsNoAssignmentWithoutLeakingAnError() {
        LocalDate date = LocalDate.now().plusDays(1);
        when(assignmentRepository
                .findByRouteIdAndScheduleIdAndServiceDateAndStatus(
                        10L, 20L, date, AssignmentStatus.ACTIVE))
                .thenReturn(Optional.empty());

        var availability = service.getAvailability(10L, 20L, date);

        assertThat(availability.isAssigned()).isFalse();
        assertThat(availability.getCapacity()).isZero();
    }

    private AssignmentRequestDto request() {
        AssignmentRequestDto request = new AssignmentRequestDto();
        request.setRouteId(10L);
        request.setScheduleId(20L);
        request.setServiceDate(LocalDate.now().plusDays(1));
        return request;
    }
}
