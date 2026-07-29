package com.transport.ticketservice.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketRequestDto {

    @NotNull(message = "User id is required")
    private Long userId;

    @NotNull(message = "Route id is required")
    private Long routeId;

    @NotNull(message = "Source stop id is required")
    private Long sourceStopId;

    @NotNull(message = "Destination stop id is required")
    private Long destinationStopId;

    @NotNull(message = "Travel date is required")
    @Future(message = "Travel date must be in the future")
    private LocalDateTime travelDate;

}
