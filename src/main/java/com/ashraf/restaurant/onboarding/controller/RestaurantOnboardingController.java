package com.ashraf.restaurant.onboarding.controller;

import com.ashraf.customer.security.CustomUserDetails;
import com.ashraf.payment.OrderResult;
import com.ashraf.restaurant.onboarding.dto.ApplicationSummaryResponse;
import com.ashraf.restaurant.onboarding.dto.RestaurantOnboardingApplicationRequest;
import com.ashraf.restaurant.onboarding.service.RestaurantOnboardingService;
import com.razorpay.RazorpayException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/restaurant/onboarding")
public class RestaurantOnboardingController {

    private final RestaurantOnboardingService onboardingService;

    public RestaurantOnboardingController(
            RestaurantOnboardingService onboardingService
    ) {
        this.onboardingService = onboardingService;
    }

    // ---------------------------------------------------------
    // SUBMIT APPLICATION
    // ---------------------------------------------------------

    @PostMapping("/applications")
    public ResponseEntity<Map<String, String>> submitApplication(
            @Valid @RequestBody RestaurantOnboardingApplicationRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {

        onboardingService.submitApplication(
                principal.getUser(),
                request
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Application submitted. Pending review."
                )
        );
    }


    // ---------------------------------------------------------
    // GET CURRENT USER'S APPLICATION
    // ---------------------------------------------------------

    @GetMapping("/applications/me")
    public ResponseEntity<ApplicationSummaryResponse> getMyApplication(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {

        return onboardingService
                .getMyApplication(principal.getUser())
                .map(ResponseEntity::ok)
                .orElse(
                        ResponseEntity.noContent().build()
                );
    }


    // ---------------------------------------------------------
    // CREATE RAZORPAY ORDER
    // ---------------------------------------------------------

    @PostMapping("/applications/{id}/payment-order")
    public ResponseEntity<OrderResult> createPaymentOrder(
            @PathVariable("id") Long applicationId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) throws RazorpayException {

        OrderResult orderResult =
                onboardingService.createPaymentOrder(
                        applicationId,
                        principal.getUser()
                );

        return ResponseEntity.ok(orderResult);
    }
}