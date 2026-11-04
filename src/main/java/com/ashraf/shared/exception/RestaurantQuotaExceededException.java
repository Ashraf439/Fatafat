package com.ashraf.shared.exception;

public class RestaurantQuotaExceededException extends RuntimeException {
    public RestaurantQuotaExceededException(String message) {
        super(message);
    }
}