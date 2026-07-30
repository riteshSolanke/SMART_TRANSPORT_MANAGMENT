package com.transport.vehicleservice.repository.projection;

import com.transport.vehicleservice.enums.AssignmentStatus;

public interface AssignmentStatusAnalyticsProjection {
    AssignmentStatus getStatus();

    Long getAssignmentCount();
}
