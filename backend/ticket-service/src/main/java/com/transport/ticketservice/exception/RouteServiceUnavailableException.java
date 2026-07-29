package com.transport.ticketservice.exception;

public class RouteServiceUnavailableException extends RuntimeException {
    public RouteServiceUnavailableException(String message) {
        super(message);
    }
    public RouteServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}