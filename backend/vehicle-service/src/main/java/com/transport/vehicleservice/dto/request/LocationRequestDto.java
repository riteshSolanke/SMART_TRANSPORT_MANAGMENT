package com.transport.vehicleservice.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class LocationRequestDto {
    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    @Digits(integer = 2, fraction = 6)
    private BigDecimal latitude;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    @Digits(integer = 3, fraction = 6)
    private BigDecimal longitude;

    @DecimalMin("0.0")
    @DecimalMax("250.0")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal speedKph;

    private LocalDateTime recordedAt;
}
