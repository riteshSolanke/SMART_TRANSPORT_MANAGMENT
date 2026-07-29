package com.transport.vehicleservice.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RouteReferenceDto {
    private Long routeId;
    private boolean active;
}
