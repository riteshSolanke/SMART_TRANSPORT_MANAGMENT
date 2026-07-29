package com.transport.vehicleservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class LocationResponseDto {
    private Long locationId;
    private Long vehicleId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal speedKph;
    private LocalDateTime recordedAt;
}
