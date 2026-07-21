package com.transport.routeservice.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class RouteSearchRequestDto {
    @NotBlank(message = "Source stop is required")
    private String from;
    @NotBlank(message = "Destination stop is required")
    private String to;
}