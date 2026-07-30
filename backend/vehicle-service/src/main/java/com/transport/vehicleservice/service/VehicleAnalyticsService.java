package com.transport.vehicleservice.service;

import com.transport.vehicleservice.dto.response.VehicleAnalyticsDto;

import java.time.LocalDate;

public interface VehicleAnalyticsService {
    VehicleAnalyticsDto summarize(LocalDate from, LocalDate to);
}
