package com.transport.routeservice.service;

import com.transport.routeservice.dto.request.StopRequestDto;
import com.transport.routeservice.dto.response.StopResponseDto;
import com.transport.routeservice.entity.Route;
import com.transport.routeservice.entity.Stop;
import com.transport.routeservice.exception.RouteNotFoundException;
import com.transport.routeservice.repository.RouteRepository;
import com.transport.routeservice.repository.StopRepository;
import com.transport.routeservice.service.impl.StopServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StopServiceImplTest {

    @Mock
    private StopRepository stopRepository;
    @Mock
    private RouteRepository routeRepository;

    private StopServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StopServiceImpl(stopRepository, routeRepository);
    }

    @Test
    void inactiveRouteCannotReceiveNewStop() {
        when(routeRepository.findByRouteIdAndActiveTrue(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addStop(10L, request(1, "0")))
                .isInstanceOf(RouteNotFoundException.class);

        verify(stopRepository, never()).save(any());
    }

    @Test
    void middleStopDistanceMustFallBetweenItsNeighbors() {
        Route route = activeRoute(10L);
        when(routeRepository.findByRouteIdAndActiveTrue(10L))
                .thenReturn(Optional.of(route));
        when(stopRepository.findByRoute_RouteIdOrderBySequenceOrderAsc(10L))
                .thenReturn(List.of(
                        stop(1L, 1, "0"),
                        stop(3L, 3, "20")));
        when(stopRepository.save(any(Stop.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StopResponseDto response = service.addStop(10L, request(2, "10"));

        assertThat(response.getSequenceOrder()).isEqualTo(2);
        assertThat(response.getDistanceFromStart())
                .isEqualByComparingTo("10");
    }

    @Test
    void rejectsMiddleStopBeyondNextStopDistance() {
        Route route = activeRoute(10L);
        when(routeRepository.findByRouteIdAndActiveTrue(10L))
                .thenReturn(Optional.of(route));
        when(stopRepository.findByRoute_RouteIdOrderBySequenceOrderAsc(10L))
                .thenReturn(List.of(
                        stop(1L, 1, "0"),
                        stop(3L, 3, "20")));

        assertThatThrownBy(() -> service.addStop(10L, request(2, "25")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("less than the next");

        verify(stopRepository, never()).save(any());
    }

    @Test
    void updateExcludesCurrentStopFromSequenceConflictCheck() {
        Route route = activeRoute(10L);
        Stop current = stop(2L, 2, "10");
        when(routeRepository.findByRouteIdAndActiveTrue(10L))
                .thenReturn(Optional.of(route));
        when(stopRepository.findByStopIdAndRoute_RouteIdAndRoute_ActiveTrue(2L, 10L))
                .thenReturn(Optional.of(current));
        when(stopRepository.findByRoute_RouteIdOrderBySequenceOrderAsc(10L))
                .thenReturn(List.of(
                        stop(1L, 1, "0"),
                        current,
                        stop(3L, 3, "20")));
        when(stopRepository.save(current)).thenReturn(current);

        StopResponseDto response = service.updateStop(
                10L, 2L, request(2, "12"));

        assertThat(response.getDistanceFromStart()).isEqualByComparingTo("12");
        verify(stopRepository).save(current);
    }

    @Test
    void deleteRemovesOnlyStopScopedToActiveRoute() {
        Route route = activeRoute(10L);
        Stop stop = stop(2L, 2, "10");
        when(routeRepository.findByRouteIdAndActiveTrue(10L))
                .thenReturn(Optional.of(route));
        when(stopRepository.findByStopIdAndRoute_RouteIdAndRoute_ActiveTrue(2L, 10L))
                .thenReturn(Optional.of(stop));

        service.deleteStop(10L, 2L);

        verify(stopRepository).delete(stop);
    }

    private Route activeRoute(Long id) {
        Route route = new Route();
        route.setRouteId(id);
        route.setActive(true);
        return route;
    }

    private StopRequestDto request(int sequence, String distance) {
        StopRequestDto dto = new StopRequestDto();
        dto.setStopName(" Stop " + sequence + " ");
        dto.setSequenceOrder(sequence);
        dto.setDistanceFromStart(new BigDecimal(distance));
        return dto;
    }

    private Stop stop(Long id, int sequence, String distance) {
        Stop stop = new Stop();
        stop.setStopId(id);
        stop.setStopName("Stop " + sequence);
        stop.setSequenceOrder(sequence);
        stop.setDistanceFromStart(new BigDecimal(distance));
        return stop;
    }
}
