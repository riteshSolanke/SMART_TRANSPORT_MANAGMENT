package com.transport.routeservice.service;

import com.transport.routeservice.service.impl.FareCalculationServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FareCalculationServiceImplTest {

    private final FareCalculationServiceImpl service =
            new FareCalculationServiceImpl();

    @Test
    void calculatesOffPeakAndPeakFare() {
        assertThat(service.calculateFare(new BigDecimal("10"), false))
                .isEqualByComparingTo("30.00");
        assertThat(service.calculateFare(new BigDecimal("10"), true))
                .isEqualByComparingTo("34.50");
    }

    @Test
    void peakWindowIncludesConfiguredBoundaries() {
        assertThat(service.isPeakHour(LocalTime.of(8, 0))).isTrue();
        assertThat(service.isPeakHour(LocalTime.of(10, 0))).isTrue();
        assertThat(service.isPeakHour(LocalTime.of(16, 59))).isFalse();
        assertThat(service.isPeakHour(LocalTime.of(19, 0))).isTrue();
    }

    @Test
    void negativeDistanceIsRejected() {
        assertThatThrownBy(() ->
                service.calculateFare(new BigDecimal("-1"), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Distance");
    }
}
