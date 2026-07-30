package com.ashraf.shared.exception;

public class SuspendedAccountException extends RuntimeException {
    public SuspendedAccountException(String message) {
        super(message);
    }
}
