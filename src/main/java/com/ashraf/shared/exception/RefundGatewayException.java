package com.ashraf.shared.exception;

public class RefundGatewayException extends RuntimeException {
    private final boolean definitive;   // true = Razorpay rejected it (4xx); false = network/5xx, safe to retry
    public RefundGatewayException(String message, boolean definitive, Throwable cause) {
        super(message, cause);
        this.definitive = definitive;
    }
    public boolean isDefinitive() { return definitive; }
}