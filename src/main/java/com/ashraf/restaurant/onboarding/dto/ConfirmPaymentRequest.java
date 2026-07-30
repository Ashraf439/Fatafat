package com.ashraf.restaurant.onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmPaymentRequest {

    @NotBlank
    private String orderId;

    @NotBlank
    private String paymentReference; // dummy value for now, real Razorpay payment ID later
}