package com.transport.paymentservice.processor;

public record ProcessorResult(boolean successful, String failureReason) {
    public static ProcessorResult success() {
        return new ProcessorResult(true, null);
    }

    public static ProcessorResult failure(String reason) {
        return new ProcessorResult(false, reason);
    }
}
