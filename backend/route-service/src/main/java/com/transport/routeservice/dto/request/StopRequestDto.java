package com.transport.routeservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
@Getter
@Setter
public class StopRequestDto {
    @NotBlank(message = "Stop name is required")
    private String stopName;
    @NotNull(message = "Sequence order is required")
    @Positive(message = "Sequence order must be positive")
    private Integer sequenceOrder;
    @NotNull(message = "Distance from start is required")
    @DecimalMin(value = "0.0", message = "Distance cannot be negative")
    private BigDecimal distanceFromStart;
}