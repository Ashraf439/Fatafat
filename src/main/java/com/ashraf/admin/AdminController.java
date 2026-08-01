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

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final RestaurantOnboardingService restaurantOnboardingService;

    public AdminController(AdminService adminService, RestaurantOnboardingService restaurantOnboardingService) {
        this.adminService = adminService;
        this.restaurantOnboardingService = restaurantOnboardingService;
    }

    // NOTE: this approves an existing Restaurant row by id. Restaurants registered
    // through /api/auth/register/restaurant no longer get a Restaurant row until
    // after onboarding + payment, so this endpoint has no current caller for that
    // flow. Left in place rather than removed unilaterally — flag if it's dead
    // weight now, or still wanted for another path (e.g. a future admin-created
    // restaurant that skips onboarding).
    @PatchMapping("/restaurants/{id}/approve")
    public ResponseEntity<Map<String, String>> approveRestaurants(@PathVariable("id") Long id) {
        adminService.approveRestaurants(id);
        return  ResponseEntity.ok(Map.of("message","Successfully approved."));
    }

    @PatchMapping("/riders/{id}/approve")
    public ResponseEntity<Map<String, String>> approveRiders(@PathVariable("id") Long id) {
        adminService.approveRiders(id);
        return  ResponseEntity.ok(Map.of("message","Successfully approved."));
    }

    @PatchMapping("/restaurant-applications/{id}/approve")
    public ResponseEntity<Map<String, String>> approveRestaurantApplication(@PathVariable("id") Long id,
                                                                            @AuthenticationPrincipal CustomUserDetails principal) {
        restaurantOnboardingService.approveApplication(id, principal.getUser());
        return ResponseEntity.ok(Map.of("message", "Application approved. Awaiting payment."));
    }

    @PatchMapping("/restaurant-applications/{id}/reject")
    public ResponseEntity<Map<String, String>> rejectRestaurantApplication(@PathVariable("id") Long id,
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