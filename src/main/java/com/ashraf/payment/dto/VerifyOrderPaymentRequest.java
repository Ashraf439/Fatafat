package com.ashraf.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyOrderPaymentRequest(
        @NotBlank String razorpayOrderId, @NotBlank String razorpayPaymentId,
        @NotBlank String razorpaySignature) {}