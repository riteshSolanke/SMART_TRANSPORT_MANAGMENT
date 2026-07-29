package com.transport.routeservice.exception;

public class DuplicateScheduleException extends RuntimeException {
    public DuplicateScheduleException(String message) {
        super(message);
    }
}
