package com.ashraf.payment.controller;

import com.ashraf.customer.security.CustomUserDetails;
import com.ashraf.payment.dto.VerifyPaymentRequest;
import com.ashraf.restaurant.core.entity.Restaurant;
import com.ashraf.restaurant.onboarding.service.RestaurantOnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final RestaurantOnboardingService onboardingService;

    public PaymentController(RestaurantOnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Restaurant restaurant = onboardingService.verifyAndConfirmPayment(
                principal.getUser(),
                request.getOrderId(),
                request.getPaymentId(),
                request.getRazorpaySignature()
        );

        return ResponseEntity.ok(
                Map.of(
                        "message", "Payment verified successfully",
                        "restaurantId", restaurant.getId().toString()
                )
        );
    }
}