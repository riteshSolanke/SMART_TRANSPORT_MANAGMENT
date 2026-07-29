package com.transport.ticketservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDto<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ApiResponseDto<T> success(String message, T data) {

        return new ApiResponseDto<>(true, message, data, LocalDateTime.now());

    }

    public static <T> ApiResponseDto<T> success(T data) {
        return new ApiResponseDto<>(true, "Success", data, LocalDateTime.now());

    }

    public static <T> ApiResponseDto<T> failure(String message) {
        return new ApiResponseDto<>(false, message, null, LocalDateTime.now());

    }

}
