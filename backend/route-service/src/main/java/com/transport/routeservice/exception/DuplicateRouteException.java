package com.transport.routeservice.exception;

public class DuplicateRouteException extends RuntimeException {
    public DuplicateRouteException(String message) {
        super(message);
    }
}
