package com.transport.routeservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ApiResponseDto<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ApiResponseDto<T> success(String message, T data, LocalDateTime timestamp) {
        return new ApiResponseDto<>(true, message, data, timestamp);
    }
    public static <T> ApiResponseDto<T> success(T data, LocalDateTime timestamp) {
        return new ApiResponseDto<>(true, "Success", data, timestamp);
    }
}