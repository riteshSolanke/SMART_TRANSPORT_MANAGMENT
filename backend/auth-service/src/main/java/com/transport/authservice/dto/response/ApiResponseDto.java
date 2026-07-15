package com.transport.authservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDto <T>{

    private boolean success;
    private String message;
    private T data;


    public static <T> ApiResponseDto <T> success(String message, T data){
        return new ApiResponseDto<>(true, message, data);
    }


    public static <T> ApiResponseDto<T> success(String message){
        return new ApiResponseDto<>(true, message, null);
    }

}
