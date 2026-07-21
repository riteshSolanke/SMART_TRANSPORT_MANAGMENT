package com.transport.routeservice.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RouteRequestDto {
    @NotBlank(message = "Route name is required")
    @Size(max = 100)
    private String routeName;
    @NotBlank(message = "Start point is required")
    @Size(max = 100)
    private String startPoint;
    @NotBlank(message = "End point is required")
    @Size(max = 100)
    private String endPoint;
}
