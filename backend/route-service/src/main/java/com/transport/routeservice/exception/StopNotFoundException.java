package com.transport.routeservice.exception;

public class StopNotFoundException extends RuntimeException {
    public StopNotFoundException(String message) {
        super(message);
    }
}
