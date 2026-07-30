package com.transport.vehicleservice.repository.projection;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface CompletedAssignmentAnalyticsProjection {
    Long getAssignmentId();

    Long getRouteId();

    Long getScheduleId();

    LocalDate getServiceDate();

    LocalDateTime getCompletedAt();
}
