package com.transport.ticketservice.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketRequestDto {

    // Optional for administrators booking on behalf of another passenger.
    // Passenger requests are always bound to the authenticated user.
    @Positive(message = "User id must be positive")
    private Long userId;

    @NotNull(message = "Route id is required")
    @Positive(message = "Route id must be positive")
    private Long routeId;

    @NotNull(message = "Schedule id is required")
    @Positive(message = "Schedule id must be positive")
    private Long scheduleId;

    @NotNull(message = "Source stop id is required")
    @Positive(message = "Source stop id must be positive")
    private Long sourceStopId;

    @NotNull(message = "Destination stop id is required")
    @Positive(message = "Destination stop id must be positive")
    private Long destinationStopId;

    @NotNull(message = "Service date is required")
    @FutureOrPresent(message = "Service date cannot be in the past")
    private LocalDate serviceDate;

    @Min(value = 1, message = "At least one passenger is required")
    @Max(value = 10, message = "At most ten passengers can be booked together")
    @Builder.Default
    private Integer passengerCount = 1;
}
