package com.ashraf.shared.exception;

public class InvalidRefreshTokenException extends RuntimeException {

    private static final String GENERIC_MESSAGE = "Invalid or expired refresh token";

    public InvalidRefreshTokenException() {
        super(GENERIC_MESSAGE);
    }
}