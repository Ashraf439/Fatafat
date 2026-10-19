package com.ashraf.admin;

import com.ashraf.customer.security.CustomUserDetails;
import com.ashraf.restaurant.onboarding.dto.ApplicationSummaryResponse;
import com.ashraf.restaurant.onboarding.dto.RejectApplicationRequest;
import com.ashraf.restaurant.onboarding.enums.RestaurantOnboardingStatus;
import com.ashraf.restaurant.onboarding.service.RestaurantOnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final RestaurantOnboardingService restaurantOnboardingService;

    public AdminController(AdminService adminService, RestaurantOnboardingService restaurantOnboardingService) {
        this.adminService = adminService;
        this.restaurantOnboardingService = restaurantOnboardingService;
    }

    @PatchMapping("/restaurants/{id}/approve")
    public ResponseEntity<Map<String, String>> approveRestaurants(@PathVariable("id") UUID id) {
        adminService.approveRestaurants(id);
        return  ResponseEntity.ok(Map.of("message","Successfully approved."));
    }

    @PatchMapping("/riders/{id}/approve")
    public ResponseEntity<Map<String, String>> approveRiders(@PathVariable("id") UUID id) {
        adminService.approveRiders(id);
        return  ResponseEntity.ok(Map.of("message","Successfully approved."));
    }

    @PatchMapping("/restaurant-applications/{id}/approve")
    public ResponseEntity<Map<String, String>> approveRestaurantApplication(@PathVariable("id") UUID id,
                                                                            @AuthenticationPrincipal CustomUserDetails principal) {
        restaurantOnboardingService.approveApplication(id, principal.getUser());
        return ResponseEntity.ok(Map.of("message", "Application approved. Awaiting payment."));
    }

    @PatchMapping("/restaurant-applications/{id}/reject")
    public ResponseEntity<Map<String, String>> rejectRestaurantApplication(@PathVariable("id") UUID id,
                                                                           @Valid @RequestBody RejectApplicationRequest req,
                                                                           @AuthenticationPrincipal CustomUserDetails principal) {
        restaurantOnboardingService.rejectApplication(id, principal.getUser(), req);
        return ResponseEntity.ok(Map.of("message", "Application rejected."));
    }
    @GetMapping("/restaurant-applications")
    public ResponseEntity<List<ApplicationSummaryResponse>> listApplications(
            @RequestParam(required = false) RestaurantOnboardingStatus status) {
        return ResponseEntity.ok(restaurantOnboardingService.listApplications(status));
    }
}