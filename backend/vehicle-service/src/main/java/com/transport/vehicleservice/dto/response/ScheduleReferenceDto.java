package com.transport.vehicleservice.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ScheduleReferenceDto {
    private Long scheduleId;
    private String daysOfWeek;
    private boolean active;
}
