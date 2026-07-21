package com.transport.routeservice.service.impl;

import com.transport.routeservice.service.FareCalculationService;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;

@Service
public class FareCalculationServiceImpl implements FareCalculationService {
    private static final BigDecimal BASE_FARE = BigDecimal.valueOf(10.0);
    private static final BigDecimal PER_KM_RATE = BigDecimal.valueOf(2.0);
    private static final BigDecimal PEAK_SURCHARGE_MULTIPLIER = BigDecimal.valueOf(1.15);

    public BigDecimal calculateFare(BigDecimal distanceKm, boolean isPeakHour) {
        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) < 0) {
            distanceKm = BigDecimal.ZERO;
        }
        BigDecimal fare = BASE_FARE.add(distanceKm.multiply(PER_KM_RATE));
        if (isPeakHour) {
            fare = fare.multiply(PEAK_SURCHARGE_MULTIPLIER);
        }
        return fare.setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isPeakHour(LocalTime time) {
        if (time == null) return false;
        boolean morningPeak = !time.isBefore(LocalTime.of(8, 0))
                && !time.isAfter(LocalTime.of(10, 0));
        boolean eveningPeak = !time.isBefore(LocalTime.of(17, 0))
                && !time.isAfter(LocalTime.of(19, 0));
        return morningPeak || eveningPeak;
    }

}
