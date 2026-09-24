package com.ashraf.shared.exception;

/**
 * A well-formed request that violates a business rule (restaurant closed, item unavailable,
 * order no longer cancellable, ...). Mapped to HTTP 422 so clients can show the message as-is.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
