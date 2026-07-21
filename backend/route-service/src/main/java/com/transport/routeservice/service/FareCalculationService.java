package com.transport.routeservice.service;

import java.math.BigDecimal;
import java.time.LocalTime;

public interface FareCalculationService {

     BigDecimal calculateFare(BigDecimal distanceKm, boolean isPeakHour);

     boolean isPeakHour(LocalTime time);
}
