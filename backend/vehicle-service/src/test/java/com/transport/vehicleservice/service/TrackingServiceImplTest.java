package com.transport.vehicleservice.service;

import com.transport.vehicleservice.dto.request.LocationRequestDto;
import com.transport.vehicleservice.entity.Vehicle;
import com.transport.vehicleservice.entity.VehicleLocation;
import com.transport.vehicleservice.enums.VehicleStatus;
import com.transport.vehicleservice.exception.LocationNotFoundException;
import com.transport.vehicleservice.repository.VehicleLocationRepository;
import com.transport.vehicleservice.repository.VehicleRepository;
import com.transport.vehicleservice.service.impl.TrackingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class TrackingServiceImplTest {
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private VehicleLocationRepository locationRepository;
    private TrackingServiceImpl service;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new TrackingServiceImpl(vehicleRepository, locationRepository);
        vehicle = new Vehicle();
        vehicle.setVehicleId(1L);
        vehicle.setActive(true);
        vehicle.setStatus(VehicleStatus.IN_SERVICE);
        when(vehicleRepository.findByVehicleIdAndActiveTrue(1L))
                .thenReturn(Optional.of(vehicle));
        when(locationRepository.save(any(VehicleLocation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void recordsLocationForVehicleInService() {
        var response = service.record(1L, request(null));

        assertThat(response.getLatitude()).isEqualByComparingTo("18.520400");
        assertThat(response.getRecordedAt()).isNotNull();
    }

    @Test
    void rejectsLocationWhenVehicleIsNotInService() {
        vehicle.setStatus(VehicleStatus.AVAILABLE);

        assertThatThrownBy(() -> service.record(1L, request(null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsLocationTimestampTooFarInFuture() {
        assertThatThrownBy(() -> service.record(
                1L, request(LocalDateTime.now().plusMinutes(6))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidHistoryLimit() {
        assertThatThrownBy(() -> service.history(1L, 201))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void returnsNewestLocationsFirstFromRepository() {
        VehicleLocation location = new VehicleLocation();
        location.setVehicle(vehicle);
        location.setLatitude(new BigDecimal("18.520400"));
        location.setLongitude(new BigDecimal("73.856700"));
        location.setRecordedAt(LocalDateTime.now());
        when(locationRepository.findByVehicle_VehicleIdOrderByRecordedAtDesc(
                any(Long.class), any(Pageable.class)))
                .thenReturn(List.of(location));

        assertThat(service.history(1L, 10)).hasSize(1);
    }

    @Test
    void reportsMissingLatestLocation() {
        when(locationRepository
                .findFirstByVehicle_VehicleIdOrderByRecordedAtDesc(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.latest(1L))
                .isInstanceOf(LocationNotFoundException.class);
    }

    private LocationRequestDto request(LocalDateTime recordedAt) {
        LocationRequestDto request = new LocationRequestDto();
        request.setLatitude(new BigDecimal("18.520400"));
        request.setLongitude(new BigDecimal("73.856700"));
        request.setSpeedKph(new BigDecimal("35.50"));
        request.setRecordedAt(recordedAt);
        return request;
    }
}
