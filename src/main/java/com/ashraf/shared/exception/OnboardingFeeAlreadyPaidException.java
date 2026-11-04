package com.ashraf.shared.exception;

public class OnboardingFeeAlreadyPaidException extends RuntimeException {
    public OnboardingFeeAlreadyPaidException(String message) {
        super(message);
    }
}