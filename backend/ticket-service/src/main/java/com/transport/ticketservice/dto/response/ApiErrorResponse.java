package com.transport.ticketservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiErrorResponse {

    private boolean success;
    private String message;
    private int status;
    private String path;
    private LocalDateTime timestamp;

}
