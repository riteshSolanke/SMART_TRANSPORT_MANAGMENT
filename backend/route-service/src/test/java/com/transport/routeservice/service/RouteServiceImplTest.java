package com.transport.routeservice.service;

import com.transport.routeservice.dto.request.RouteRequestDto;
import com.transport.routeservice.dto.response.FareResponseDto;
import com.transport.routeservice.dto.response.RouteResponseDto;
import com.transport.routeservice.dto.response.RouteSearchResponseDto;
import com.transport.routeservice.entity.Route;
import com.transport.routeservice.entity.Schedule;
import com.transport.routeservice.entity.Stop;
import com.transport.routeservice.repository.RouteRepository;
import com.transport.routeservice.repository.ScheduleRepository;
import com.transport.routeservice.repository.StopRepository;
import com.transport.routeservice.service.impl.RouteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteServiceImplTest {

    @Mock
    private RouteRepository routeRepository;
    @Mock
    private FareCalculationService fareCalculationService;
    @Mock
    private StopRepository stopRepository;
    @Mock
    private ScheduleRepository scheduleRepository;

    private RouteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RouteServiceImpl(
                routeRepository,
                fareCalculationService,
                stopRepository,
                scheduleRepository);
    }

    @Test
    void searchFindsIntermediateStopsInForwardDirection() {
        Route route = routeWithStopsAndSchedules();
        when(routeRepository.findRoutesServingStopsInOrder("Alpha", "Gamma"))
                .thenReturn(List.of(route));
        when(fareCalculationService.calculateFare(
                new BigDecimal("12.00"), false))
                .thenReturn(new BigDecimal("34.00"));

        List<RouteSearchResponseDto> results = service.searchRoutes(
                " Alpha ", "Gamma", LocalDate.of(2026, 7, 27));

        assertThat(results).hasSize(1);
        RouteSearchResponseDto result = results.get(0);
        assertThat(result.getSourceStopId()).isEqualTo(1L);
        assertThat(result.getDestinationStopId()).isEqualTo(3L);
        assertThat(result.getTotalDistanceKm()).isEqualByComparingTo("12.00");
        assertThat(result.getAvailableSchedules())
                .extracting("scheduleId")
                .containsExactly(100L);
        verify(routeRepository).findRoutesServingStopsInOrder("Alpha", "Gamma");
    }

    @Test
    void searchDefensivelyRejectsReverseStopOrder() {
        Route route = routeWithStopsAndSchedules();
        when(routeRepository.findRoutesServingStopsInOrder("Gamma", "Alpha"))
                .thenReturn(List.of(route));

        List<RouteSearchResponseDto> results =
                service.searchRoutes("Gamma", "Alpha", null);

        assertThat(results).isEmpty();
    }

    @Test
    void searchFiltersSchedulesByTravelDay() {
        Route route = routeWithStopsAndSchedules();
        when(routeRepository.findRoutesServingStopsInOrder("Alpha", "Gamma"))
                .thenReturn(List.of(route));

        List<RouteSearchResponseDto> results = service.searchRoutes(
                "Alpha", "Gamma", LocalDate.of(2026, 7, 28));

        assertThat(results).isEmpty();
    }

    @Test
    void fareUsesOnlySelectedSchedulesDepartureTime() {
        Route route = routeWithStopsAndSchedules();
        Stop source = route.getStops().get(0);
        Stop destination = route.getStops().get(2);
        Schedule selected = route.getSchedules().get(0);
        when(routeRepository.findByRouteIdAndActiveTrue(10L))
                .thenReturn(Optional.of(route));
        when(stopRepository.findByStopIdAndRoute_RouteIdAndRoute_ActiveTrue(1L, 10L))
                .thenReturn(Optional.of(source));
        when(stopRepository.findByStopIdAndRoute_RouteIdAndRoute_ActiveTrue(3L, 10L))
                .thenReturn(Optional.of(destination));
        when(scheduleRepository
                .findByScheduleIdAndRoute_RouteIdAndActiveTrue(100L, 10L))
                .thenReturn(Optional.of(selected));
        when(fareCalculationService.isPeakHour(LocalTime.of(9, 0)))
                .thenReturn(true);
        when(fareCalculationService.calculateFare(
                new BigDecimal("12.00"), true))
                .thenReturn(new BigDecimal("39.10"));

        FareResponseDto response =
                service.getFare(10L, 1L, 3L, 100L, null);

        assertThat(response.getScheduleId()).isEqualTo(100L);
        assertThat(response.getDepartureTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(response.isPeakHourApplied()).isTrue();
        assertThat(response.getFare()).isEqualByComparingTo("39.10");
    }

    @Test
    void fareRejectsReverseDirectionEvenWhenDistancesLookValid() {
        Route route = routeWithStopsAndSchedules();
        Stop source = route.getStops().get(2);
        Stop destination = route.getStops().get(0);
        when(routeRepository.findByRouteIdAndActiveTrue(10L))
                .thenReturn(Optional.of(route));
        when(stopRepository.findByStopIdAndRoute_RouteIdAndRoute_ActiveTrue(3L, 10L))
                .thenReturn(Optional.of(source));
        when(stopRepository.findByStopIdAndRoute_RouteIdAndRoute_ActiveTrue(1L, 10L))
                .thenReturn(Optional.of(destination));

        assertThatThrownBy(() ->
                service.getFare(10L, 3L, 1L, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must come after");
    }

    @Test
    void createRouteTrimsValuesAndRejectsEqualEndpoints() {
        RouteRequestDto invalid = routeRequest("Line", "Central", " central ");

        assertThatThrownBy(() -> service.createRoute(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be different");
        verify(routeRepository, never()).save(
                org.mockito.ArgumentMatchers.any(Route.class));

        RouteRequestDto valid = routeRequest(
                " City Line ", " Alpha ", " Gamma ");
        when(routeRepository.existsByRouteNameIgnoreCase("City Line"))
                .thenReturn(false);
        when(routeRepository.save(
                org.mockito.ArgumentMatchers.any(Route.class)))
                .thenAnswer(invocation -> {
                    Route saved = invocation.getArgument(0);
                    saved.setRouteId(10L);
                    return saved;
                });

        RouteResponseDto response = service.createRoute(valid);

        assertThat(response.getRouteName()).isEqualTo("City Line");
        assertThat(response.getStartPoint()).isEqualTo("Alpha");
        assertThat(response.getEndPoint()).isEqualTo("Gamma");
    }

    @Test
    void deleteRouteAlsoDeactivatesItsSchedules() {
        Route route = routeWithStopsAndSchedules();
        when(routeRepository.findByRouteIdAndActiveTrue(10L))
                .thenReturn(Optional.of(route));

        service.deleteRoute(10L);

        assertThat(route.isActive()).isFalse();
        assertThat(route.getSchedules())
                .allMatch(schedule -> !schedule.isActive());
        verify(routeRepository).save(route);
    }

    private Route routeWithStopsAndSchedules() {
        Route route = new Route();
        route.setRouteId(10L);
        route.setRouteName("City Line");
        route.setStartPoint("Alpha");
        route.setEndPoint("Gamma");
        route.setActive(true);
        route.setStops(new ArrayList<>(List.of(
                stop(1L, "Alpha", 1, "0.00"),
                stop(2L, "Beta", 2, "5.00"),
                stop(3L, "Gamma", 3, "12.00"))));

        Schedule monday = new Schedule();
        monday.setScheduleId(100L);
        monday.setDepartureTime(LocalTime.of(9, 0));
        monday.setArrivalTime(LocalTime.of(10, 0));
        monday.setDaysOfWeek("MON");
        monday.setActive(true);

        Schedule inactive = new Schedule();
        inactive.setScheduleId(101L);
        inactive.setDepartureTime(LocalTime.of(11, 0));
        inactive.setArrivalTime(LocalTime.NOON);
        inactive.setDaysOfWeek("TUE");
        inactive.setActive(false);
        route.setSchedules(new ArrayList<>(List.of(monday, inactive)));
        return route;
    }

    private Stop stop(
            Long id, String name, int sequence, String distance) {
        Stop stop = new Stop();
        stop.setStopId(id);
        stop.setStopName(name);
        stop.setSequenceOrder(sequence);
        stop.setDistanceFromStart(new BigDecimal(distance));
        return stop;
    }

    private RouteRequestDto routeRequest(
            String name, String startPoint, String endPoint) {
        RouteRequestDto dto = new RouteRequestDto();
        dto.setRouteName(name);
        dto.setStartPoint(startPoint);
        dto.setEndPoint(endPoint);
        return dto;
    }
}
