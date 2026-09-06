package com.ashraf.restaurant.staff.controller;

import com.ashraf.core.service.PermissionService;
import com.ashraf.customer.security.CustomUserDetails;
import com.ashraf.restaurant.staff.dto.StaffAddRequest;
import com.ashraf.restaurant.staff.dto.StaffPermissionRequest;
import com.ashraf.restaurant.staff.dto.StaffResponse;
import com.ashraf.restaurant.staff.service.RestaurantStaffService;
import com.ashraf.shared.exception.AccessDeniedException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/restaurant/staff")
public class RestaurantStaffController {

    private final RestaurantStaffService restaurantStaffService;
    private final PermissionService permissionService;

    public RestaurantStaffController(RestaurantStaffService restaurantStaffService,
                                     PermissionService permissionService) {
        this.restaurantStaffService = restaurantStaffService;
        this.permissionService = permissionService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> addStaff(@RequestBody @Valid StaffAddRequest request,
                                                        @AuthenticationPrincipal CustomUserDetails principal) {
        requirePermission(principal, "STAFF_ADD");
        restaurantStaffService.addStaff(principal.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Added staff"));
    }

    @GetMapping
    public ResponseEntity<List<StaffResponse>> listStaff(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(restaurantStaffService.listStaff(principal.getUser()));
    }

    @DeleteMapping("/{staffUserId}")
    public ResponseEntity<Map<String, String>> removeStaff(@AuthenticationPrincipal CustomUserDetails principal,
                                                           @PathVariable Long staffUserId) {
        requirePermission(principal, "STAFF_REMOVE");
        restaurantStaffService.removeStaff(principal.getUser(), staffUserId);
        return ResponseEntity.ok(Map.of("message", "Removed the staff"));
    }

    @PatchMapping("/{staffUserId}/permissions")
    public ResponseEntity<Map<String, String>> updateStaffPermission(@RequestBody @Valid StaffPermissionRequest request,
                                                                     @AuthenticationPrincipal CustomUserDetails principal,
                                                                     @PathVariable Long staffUserId) {
        requirePermission(principal, "STAFF_MANAGE_PERMISSIONS");
        restaurantStaffService.updateStaffPermission(principal.getUser(), staffUserId, request);
        return ResponseEntity.ok(Map.of("message", "Updated staff permission"));
    }

    private void requirePermission(CustomUserDetails principal, String permissionName) {
        if (!permissionService.hasPermission(principal.getUser().getId(), permissionName)) {
            throw new AccessDeniedException("No access");
        }
    }
}