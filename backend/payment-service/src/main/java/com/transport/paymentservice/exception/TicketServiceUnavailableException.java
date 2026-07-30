package com.transport.paymentservice.exception;

public class TicketServiceUnavailableException extends RuntimeException {
    public TicketServiceUnavailableException(String message) {
        super(message);
    }
}
