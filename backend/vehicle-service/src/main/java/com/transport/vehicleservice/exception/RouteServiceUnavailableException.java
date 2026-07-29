package com.transport.vehicleservice.exception;

public class RouteServiceUnavailableException extends RuntimeException {
    public RouteServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
