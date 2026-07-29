package com.transport.vehicleservice.exception;

public class RouteReferenceException extends RuntimeException {
    public RouteReferenceException(String message) {
        super(message);
    }

    public RouteReferenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
