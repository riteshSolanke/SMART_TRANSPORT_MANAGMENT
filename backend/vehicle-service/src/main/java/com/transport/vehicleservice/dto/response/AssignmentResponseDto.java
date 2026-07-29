package com.transport.vehicleservice.dto.response;

import com.transport.vehicleservice.enums.AssignmentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class AssignmentResponseDto {
    private Long assignmentId;
    private Long vehicleId;
    private Long routeId;
    private Long scheduleId;
    private LocalDate serviceDate;
    private AssignmentStatus status;
    private LocalDateTime assignedAt;
    private LocalDateTime completedAt;
}
