package com.transport.ticketservice.exception;

public class VehicleServiceUnavailableException extends RuntimeException {
    public VehicleServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
