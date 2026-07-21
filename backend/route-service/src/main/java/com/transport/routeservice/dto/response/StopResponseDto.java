package com.transport.routeservice.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
@Getter
@Builder
@AllArgsConstructor
public class StopResponseDto {
    private Long stopId;
    private String stopName;
    private Integer sequenceOrder;
    private BigDecimal distanceFromStart;
}