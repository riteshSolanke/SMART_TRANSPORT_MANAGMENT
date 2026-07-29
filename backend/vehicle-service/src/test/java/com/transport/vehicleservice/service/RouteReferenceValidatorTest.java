package com.transport.vehicleservice.service;

import com.transport.vehicleservice.client.RouteServiceClient;
import com.transport.vehicleservice.dto.response.ApiResponseDto;
import com.transport.vehicleservice.dto.response.RouteReferenceDto;
import com.transport.vehicleservice.dto.response.ScheduleReferenceDto;
import com.transport.vehicleservice.exception.RouteReferenceException;
import com.transport.vehicleservice.service.impl.RouteReferenceValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class RouteReferenceValidatorTest {
    @Mock
    private RouteServiceClient client;
    private RouteReferenceValidator validator;
    private final LocalDate monday = LocalDate.of(2026, 8, 3);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new RouteReferenceValidator(client);
        when(client.getRoute(10L)).thenReturn(routeResponse(true));
        when(client.getSchedule(10L, 20L))
                .thenReturn(scheduleResponse(true, "MON,WED,FRI"));
    }

    @Test
    void acceptsActiveScheduleOperatingOnServiceDate() {
        assertThatCode(() -> validator.validate(10L, 20L, monday))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsInactiveRoute() {
        when(client.getRoute(10L)).thenReturn(routeResponse(false));

        assertThatThrownBy(() -> validator.validate(10L, 20L, monday))
                .isInstanceOf(RouteReferenceException.class)
                .hasMessageContaining("Route is not active");
    }

    @Test
    void rejectsInactiveSchedule() {
        when(client.getSchedule(10L, 20L))
                .thenReturn(scheduleResponse(false, "DAILY"));

        assertThatThrownBy(() -> validator.validate(10L, 20L, monday))
                .isInstanceOf(RouteReferenceException.class)
                .hasMessageContaining("Schedule is not active");
    }

    @Test
    void rejectsScheduleThatDoesNotOperateOnDate() {
        when(client.getSchedule(10L, 20L))
                .thenReturn(scheduleResponse(true, "TUE,THU"));

        assertThatThrownBy(() -> validator.validate(10L, 20L, monday))
                .isInstanceOf(RouteReferenceException.class)
                .hasMessageContaining("does not operate");
    }

    @Test
    void acceptsDailySchedule() {
        when(client.getSchedule(10L, 20L))
                .thenReturn(scheduleResponse(true, "DAILY"));

        assertThatCode(() -> validator.validate(10L, 20L, monday))
                .doesNotThrowAnyException();
    }

    private ApiResponseDto<RouteReferenceDto> routeResponse(boolean active) {
        RouteReferenceDto route = new RouteReferenceDto();
        route.setRouteId(10L);
        route.setActive(active);
        return new ApiResponseDto<>(
                true, "Success", route, LocalDateTime.now());
    }

    private ApiResponseDto<ScheduleReferenceDto> scheduleResponse(
            boolean active, String days) {
        ScheduleReferenceDto schedule = new ScheduleReferenceDto();
        schedule.setScheduleId(20L);
        schedule.setActive(active);
        schedule.setDaysOfWeek(days);
        return new ApiResponseDto<>(
                true, "Success", schedule, LocalDateTime.now());
    }
}
