package com.ashraf.restaurant.onboarding.controller;

import com.ashraf.customer.security.CustomUserDetails;
import com.ashraf.restaurant.core.entity.Restaurant;
import com.ashraf.restaurant.onboarding.dto.ApplicationSummaryResponse;
import com.ashraf.restaurant.onboarding.dto.ConfirmPaymentRequest;
import com.ashraf.restaurant.onboarding.dto.RestaurantOnboardingApplicationRequest;
import com.ashraf.restaurant.payment.OrderResult;
import com.ashraf.restaurant.onboarding.service.RestaurantOnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/restaurant/onboarding")
public class RestaurantOnboardingController {

    private final RestaurantOnboardingService onboardingService;

    public RestaurantOnboardingController(RestaurantOnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    // authenticated — user must have verified their email and logged in first
    @PostMapping("/applications")
    public ResponseEntity<Map<String, String>> submitApplication(
            @Valid @RequestBody RestaurantOnboardingApplicationRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        onboardingService.submitApplication(principal.getUser(), request);
        return ResponseEntity.ok(Map.of("message", "Application submitted. Pending review."));
    }

    @PostMapping("/applications/{id}/payment-order")
    public ResponseEntity<OrderResult> createPaymentOrder(
            @PathVariable("id") Long applicationId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        OrderResult orderResult = onboardingService.createPaymentOrder(applicationId, principal.getUser());
        return ResponseEntity.ok(orderResult);
    }

    // payment gateway calls this directly — no JWT, must be permitAll() in SecurityConfig
    // TODO: verify webhook signature before calling confirmPayment(); currently unguarded
    @PostMapping("/payments/webhook")
    public ResponseEntity<Void> confirmPayment(@Valid @RequestBody ConfirmPaymentRequest request) {
        Restaurant restaurant = onboardingService.confirmPayment(request);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/applications/me")
    public ResponseEntity<ApplicationSummaryResponse> getMyApplication(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return onboardingService.getMyApplication(principal.getUser())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}