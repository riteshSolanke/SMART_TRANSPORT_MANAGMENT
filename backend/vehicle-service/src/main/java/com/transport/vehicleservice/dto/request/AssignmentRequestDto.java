package com.transport.vehicleservice.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AssignmentRequestDto {
    @NotNull
    @Positive
    private Long routeId;

    @NotNull
    @Positive
    private Long scheduleId;

    @NotNull
    @FutureOrPresent
    private LocalDate serviceDate;
}
